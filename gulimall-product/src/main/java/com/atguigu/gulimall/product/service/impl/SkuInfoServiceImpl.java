package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.SkuItemSaleAttrVo;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


@Slf4j
@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.save(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        String key = (String) params.get("key");
        String catelogId = (String) params.get("catelogId");
        String brandId = (String) params.get("brandId");
        String minPrice = (String) params.get("min");
        String maxPrice = (String) params.get("max");


        QueryWrapper<SkuInfoEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(catelogId) && !catelogId.equals("0")) {
            queryWrapper.eq("catalog_id", catelogId);
        }

        if (StringUtils.isNotEmpty(brandId) && !brandId.equals("0")) {
            queryWrapper.eq("brand_id", brandId);
        }

        if (StringUtils.isNotEmpty(key)) {
            queryWrapper.and(wrapper -> wrapper.eq("id", key).or().like("spu_name", key));
        }

        if (StringUtils.isNotEmpty(minPrice)) {
            try {
                //实体类里面的价格类型是BigDecimal类型的，所以要先把用户传过来的价格变为BigDecimal类型再进行比较
                BigDecimal bigDecimal = new BigDecimal(minPrice);
                if (bigDecimal.compareTo(new BigDecimal("0")) > 0) {
                    queryWrapper.ge("price", minPrice);
                }
            } catch (Exception e) {
                log.info("用户传入了错误的价格:{}", maxPrice);
                throw new RuntimeException("价格填写错误");
            }

        }

        if (StringUtils.isNotEmpty(maxPrice)) {
            try {
                BigDecimal bigDecimal = new BigDecimal(maxPrice);
                if (bigDecimal.compareTo(new BigDecimal("0")) > 0) {
                    queryWrapper.le("price", maxPrice);
                }
            } catch (Exception e) {
                log.info("用户传入了错误的价格:{}", maxPrice);
                throw new RuntimeException("价格填写错误");
            }


        }


        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 通过spuId查询到sku的信息
     *
     * @param spuId
     * @return
     */
    @Override
    public List<SkuInfoEntity> getSkuBySpuId(Long spuId) {
        QueryWrapper<SkuInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spu_id", spuId);
        List<SkuInfoEntity> list = this.list(queryWrapper);
        return list;
    }

    @Override
    public SkuItemVo item(Long skuId) {
        SkuItemVo skuItemVo = new SkuItemVo();

        /*
            1 获取sku基本信息
            执行第一个异步任务，设置skuinfo,并且我们还需要这个skuinfo留给后面使用，所以用supply
         */
        CompletableFuture<SkuInfoEntity> skuInfoEntityCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfoEntity skuInfoEntity = getById(skuId);
            skuItemVo.setInfo(skuInfoEntity);
            return skuInfoEntity;
        }, threadPoolExecutor);      //使用我们自己的线程池


        CompletableFuture<Void> saleAttrFuture = skuInfoEntityCompletableFuture.thenAcceptAsync((res) -> {
            /*
                2  获取spu的销售属性组合
                这步必须在第一步之后执行，因为他需要获取第一步执行完之后的结果才能执行，所以调用的时候使用的第一步的结果调用的
             */
            List<SkuItemSaleAttrVo> skuItemSaleAttrVoList = skuSaleAttrValueService.getSaleAttrsBySpuId(res.getSpuId());
            skuItemVo.setSaleAttr(skuItemSaleAttrVoList);
        }, threadPoolExecutor);


        CompletableFuture<Void> descFuture = skuInfoEntityCompletableFuture.thenAcceptAsync((res) -> {
            /*
                3 获取spu的介绍
                这步必须在第一步之后执行，因为他需要获取第一步执行完之后的结果才能执行，所以调用的时候使用的第一步的结果调用的
             */
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesc(spuInfoDesc);
        }, threadPoolExecutor);


        CompletableFuture<Void> baseFuture = skuInfoEntityCompletableFuture.thenAcceptAsync((res) -> {
            /*
                获取spu的规格参数信息(获取所有属性分组，以及属性分组里面对应的所有值)
                这步必须在第一步之后执行，因为他需要获取第一步执行完之后的结果才能执行，所以调用的时候使用的第一步的结果调用的
             */
            List<SpuItemAttrGroupVo> spuItemAttrGroupVoList = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setGroupAttrs(spuItemAttrGroupVoList);
        }, threadPoolExecutor);


        //这个任务和前面的那几个任务没有联系，所以可以重新开启一个异步任务来执行
        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            //获取sku图片信息
            List<SkuImagesEntity> skuImagesEntities = skuImagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(skuImagesEntities);
        }, threadPoolExecutor);

        CompletableFuture.allOf(skuInfoEntityCompletableFuture,
                        saleAttrFuture,
                        descFuture,
                        baseFuture,
                        imageFuture)
                .join();


        return skuItemVo;
    }

    /**
     * 获取商品价格
     *
     * @param skuId
     * @return
     */
    @Override
    public BigDecimal getPrice(Long skuId) {
        SkuInfoEntity skuInfoEntity = getById(skuId);
        return skuInfoEntity.getPrice();
    }


}