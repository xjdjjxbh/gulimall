package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.ProductConstant;
import com.atguigu.common.to.SkuEsModel;
import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.to.SpuBoundTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.dao.SpuInfoDao;
import com.atguigu.gulimall.product.entity.*;
import com.atguigu.gulimall.product.feign.CouponFeignService;
import com.atguigu.gulimall.product.feign.SearchFeignService;
import com.atguigu.gulimall.product.feign.WareFeignService;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private SpuImagesService spuImagesService;
    @Autowired
    private AttrServiceImpl attrService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    @Autowired
    private SkuInfoService skuInfoService;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private CouponFeignService couponFeignService;


    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /*要调用这个事务方法，必须使用代理对象来调用，不然事务就会失效
    不用管@Transactional注解里面的代码是怎么调用的，只需要知道带了@Transaction注解的方法需要被
    代理对象调用，事务才会生效
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo spuSaveVo) {
        //1 保存spu的基本信息  pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(spuSaveVo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(spuInfoEntity);

        //2 保存spu的描述图片  pms_spu_info_desc
        List<String> decripts = spuSaveVo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",", decripts));
        spuInfoDescService.saveSpuInfoDescribes(spuInfoDescEntity);

        //3 保存spu的图片集   pms_spu_images
        SpuImagesEntity spuImagesEntity = new SpuImagesEntity();
        spuImagesEntity.setSpuId(spuInfoEntity.getId());
        List<String> images = spuSaveVo.getImages();
        spuImagesService.saveSpuImages(spuInfoEntity.getId(), images);

        //4 保存spu的规格参数 pms_product_attr_value
        List<BaseAttrs> baseAttrs = spuSaveVo.getBaseAttrs();
        List<ProductAttrValueEntity> productAttrValueEntities = baseAttrs.stream().map(baseAttr -> {
            ProductAttrValueEntity productAttrValueEntity = new ProductAttrValueEntity();
            productAttrValueEntity.setSpuId(spuInfoEntity.getId());
            productAttrValueEntity.setAttrId(baseAttr.getAttrId());
            AttrEntity attrEntity = attrService.getById(baseAttr.getAttrId());
            productAttrValueEntity.setAttrName(attrEntity.getAttrName());
            productAttrValueEntity.setAttrValue(baseAttr.getAttrValues());
            productAttrValueEntity.setQuickShow(baseAttr.getShowDesc());
            return productAttrValueEntity;
        }).collect(Collectors.toList());
        productAttrValueService.saveProductAttr(productAttrValueEntities);

        Bounds bounds = spuSaveVo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.saveCouponBounds(spuBoundTo);
        if (r.getCode() != 0) {
            log.error("远程保存spu积分信息失败");
        }

        //5 保存spu对应的所有sku信息
        //5.1  保存sku的基本信息  pms_sku_info
        List<Skus> skus = spuSaveVo.getSkus();
        if (skus != null && !skus.isEmpty()) {
            skus.forEach(sku -> {

                String defaultImg = "";
                for (Images image : sku.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defaultImg = image.getImgUrl();
                    }
                }

                /*先要保存sku的信息之后才能保存sku里面的所有图片信息
                （因为这些图片信息需要有sku的id才能保存，而只有在保存了sku信息之后才能生成sku的图片信息）
                sku里面涉及到了默认图片，所以要先把默认图片的url找出来之后再保存sku的信息
                 */
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku, skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                skuInfoService.saveSkuInfo(skuInfoEntity);

                Long skuId = skuInfoEntity.getSkuId();

                List<SkuImagesEntity> skuImagesEntities = sku.getImages().stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(image.getImgUrl());
                    skuImagesEntity.setDefaultImg(image.getDefaultImg());
                    return skuImagesEntity;
                }).filter(img -> {
                    return !StringUtils.isEmpty(img.getImgUrl());
                }).collect(Collectors.toList());

                /*5.2  保存sku的图片信息  pms_sku_images
                    前端提交的是全量图片，如果用户提交的图片不足8张，前端会把不足的部分当做空数据提交过来
                 */
                skuImagesService.saveBatch(skuImagesEntities);

                //5.3  保存sku的销售属性信息  pms_sku_sale_attr_value
                List<Attr> attrs = sku.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attrs.stream().map(attr -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku, skuReductionTo);
                skuReductionTo.setSkuId(skuId);

                /*
                如果存在满几件打折 或者存在 满多少减多少这种情况才会去调用远程服务
                这样可以避免存储无意义的数据
                 */
                if (skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal(0)) > 0) {
                    R r1 = couponFeignService.saveSkuRedution(skuReductionTo);
                    if (r1.getCode() != 0) {
                        log.error("远程保存sku优惠信息失败");
                    }
                }

            });
        }
    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        /*
         *不用担心key取出来是null，强制转换的时候报错，因为null可以转换为任何类型，例如
         * String key = null;
         * Object key = null;
         * Integer key = null;
         * 这些都是合法的
         *
         * get出来了之后不能直接使用toString方法，因为如果取出来的对象是null，那么toString方法会报错
         */
        String key = (String) params.get("key");
        String catelogId = (String) params.get("catelogId");
        String brandId = (String) params.get("brandId");
        String status = (String) params.get("status");

        QueryWrapper<SpuInfoEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(catelogId) && !catelogId.equals("0")) {
            queryWrapper.eq("catalog_id", catelogId);
        }

        if (StringUtils.isNotEmpty(brandId) && !brandId.equals("0")) {
            queryWrapper.eq("brand_id", brandId);
        }

        if (StringUtils.isNotEmpty(status)) {
            queryWrapper.eq("publish_status", status);
        }

        if (StringUtils.isNotEmpty(key)) {
            queryWrapper.and(wrapper -> wrapper.eq("id", key).or().like("spu_name", key));
        }


        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);

    }

    /**
     * 商品上架
     *
     * @param spuId
     */
    @Transactional
    @Override
    public void spuUp(Long spuId) {

        //1 查询当前spuid所对应的所有sku信息，品牌的名字
        List<SkuInfoEntity> skus = skuInfoService.getSkuBySpuId(spuId);
        List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

        //查询当前sku的所有可以被检索的规格属性（因为规格属性是spu决定的，而不是sku，所以只用查询一遍）
        List<ProductAttrValueEntity> productAttrValueEntities = productAttrValueService.baseAttrListForSpu(spuId);
        List<Long> attrIds = productAttrValueEntities.stream().map(ProductAttrValueEntity::getAttrId).collect(Collectors.toList());

        List<Long> searchAttrIds = attrService.selectSearchAttrs(attrIds);
        HashSet<Long> set = new HashSet<>(searchAttrIds);

        List<SkuEsModel.Attrs> attrList = productAttrValueEntities.stream().filter(
                productAttrValueEntity -> set.contains(productAttrValueEntity.getAttrId())
        ).map(item -> {
            SkuEsModel.Attrs attrs = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrs);
            return attrs;
        }).collect(Collectors.toList());

        // 请求远程服务，查询是否有库存
        HashMap<Long, Boolean> stockMap = new HashMap<>();
        try {
            R skuHasStock = wareFeignService.getHasStock(skuIds);
            List<SkuHasStockVo> skuHasStockVos = skuHasStock.getData(new TypeReference<ArrayList<SkuHasStockVo>>() {
            });

            skuHasStockVos.forEach(item -> {
                stockMap.put(item.getSkuId(), item.getHasStock());
            });
        } catch (Exception e) {
            log.error("远程调用查询库存服务失败，原因:{}", e.getMessage());
        }


        //2 封装sku的信息
        List<SkuEsModel> skuEsModels = skus.stream().map(skuInfoEntity -> {


            SkuEsModel skuEsModel = new SkuEsModel();
            BeanUtils.copyProperties(skuInfoEntity, skuEsModel);

            skuEsModel.setSkuImg(skuInfoEntity.getSkuDefaultImg());
            skuEsModel.setSkuPrice(skuInfoEntity.getPrice());

            //设置库存信息   如果远程调用失败，那么默认设置为有库存
            if (stockMap.isEmpty()) {
                skuEsModel.setHasStock(true);
            } else {
                skuEsModel.setHasStock(stockMap.get(skuInfoEntity.getSkuId()));
            }


            // 设置热度评分
            skuEsModel.setHotScore(0L);

            // 查询品牌和分类名字
            BrandEntity brandEntity = brandService.getById(skuInfoEntity.getBrandId());
            skuEsModel.setBrandName(brandEntity.getName());
            skuEsModel.setBrandImg(brandEntity.getLogo());

            CategoryEntity categoryEntity = categoryService.getById(skuInfoEntity.getCatalogId());
            skuEsModel.setCatalogName(categoryEntity.getName());

            //设置检索属性
            skuEsModel.setAttrs(attrList);

            return skuEsModel;
        }).collect(Collectors.toList());


        // 将查询出来的数据发送给es进行保存
        R r = searchFeignService.productStatueUp(skuEsModels);
        if (r.getCode() != 0) {
            log.error("向elasticsearch中保存数据失败");
        } else {
            //修改当前spu的状态为已上架，只有把spu下的所有sku都上传成功之后才能把状态修改为上架
            this.baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        }


    }

}