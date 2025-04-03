package com.atguigu.gulimall.product.app;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.ProductAttrValueEntity;
import com.atguigu.gulimall.product.service.AttrAttrgroupRelationService;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.gulimall.product.service.ProductAttrValueService;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


/**
 * 商品属性
 *
 * @author liuchong
 * @email sunlightcs@gmail.com
 * @date 2025-01-22 20:07:13
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    @Autowired
    private AttrAttrgroupRelationService attrAttrgroupRelationService;


    @Autowired
    private ProductAttrValueService productAttrValueService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }

//    /**
//     * 获取分类规格参数
//     */
//    @GetMapping("/base/list/{catelogId}")
//    public R list(@PathVariable("catelogId") Long catelogId,@RequestParam Map<String, Object> params){
//        PageUtils page = attrService.queryBaseAttr(params,catelogId);
//        return R.ok().put("page", page);
//    }

    /**
     * 获取分类销售/详情属性
     * 前端获取基本属性和销售属性的逻辑都是一样的，所以直接共用这同一个方法
     */
    @GetMapping("/{attrType}/list/{catelogId}")
    public R sale(@PathVariable Long catelogId
            , @PathVariable("attrType") String attrType
            , @RequestParam Map<String, Object> params) {
        PageUtils page = attrService.queryBaseAttr(params, catelogId, attrType);
        return R.ok().put("page", page);
    }


    /**
     * 获取属性详情
     */
    @GetMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId) {
//		AttrEntity attr = attrService.getById(attrId);
        AttrRespVo attr = attrService.getDetail(attrId);

        return R.ok().put("attr", attr);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrVo attrVo) {
        attrService.saveAttr(attrVo);

        return R.ok();
    }

    /**
     * 修改属性详细信息
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVo attrVo) {
        attrService.updateDetail(attrVo);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody List<Long> attrIds) {
        attrService.removeByIds(attrIds);
        attrAttrgroupRelationService.remove(new QueryWrapper<AttrAttrgroupRelationEntity>()
                .in("attr_id", attrIds));

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/base/listforspu/{spuId}")
    public R baseAttrListForSpu(@PathVariable("spuId") Long spuId) {
        List<ProductAttrValueEntity> list = productAttrValueService.baseAttrListForSpu(spuId);
        return R.ok().put("data", list);
    }


    /**
     * 修改商品规格
     */
    @PostMapping("/update/{spuId}")
    public R updateProductAttr(@PathVariable("spuId") Long spuId,
                                @RequestBody List<ProductAttrValueEntity> productattrvalueEntities
    ) {
        productAttrValueService.updateProductAttr(spuId,productattrvalueEntities);
        return R.ok();
    }


}
