package com.atguigu.gulimall.product.service;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author liuchong
 * @email sunlightcs@gmail.com
 * @date 2025-01-22 20:07:13
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 新增品牌于分类关联关系
     *
     */
    void saveDetail(CategoryBrandRelationEntity categoryBrandRelation);


    void updateBrand(Long brandId, String brandName);

    void updateCategory(Long catId, String categoryName);

    /**
     * 获取分类下面所有的商品品牌
     * @param catId
     * @return
     */
    List<BrandEntity> getRelatedBrands(Long catId);
}

