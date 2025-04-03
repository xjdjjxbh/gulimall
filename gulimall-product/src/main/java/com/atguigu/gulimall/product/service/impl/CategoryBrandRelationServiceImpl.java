package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.BrandDao;
import com.atguigu.gulimall.product.dao.CategoryBrandRelationDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Autowired
    private BrandDao brandDao;

    @Autowired
    private CategoryDao categoryDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 新增品牌于分类关联关系
     */
    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        Long brandId = categoryBrandRelation.getBrandId();
        Long catelogId = categoryBrandRelation.getCatelogId();

        BrandEntity brandEntity = brandDao.selectById(brandId);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        CategoryBrandRelationEntity relationEntity = new CategoryBrandRelationEntity();

        relationEntity.setBrandId(brandId);
        relationEntity.setCatelogId(catelogId);
        relationEntity.setBrandName(brandEntity.getName());
        relationEntity.setCatelogName(categoryEntity.getName());

        this.save(relationEntity);
    }

    @Override
    public void updateBrand(Long brandId, String brandName) {
        CategoryBrandRelationEntity relationEntity = new CategoryBrandRelationEntity();
        relationEntity.setBrandId(brandId);
        relationEntity.setBrandName(brandName);

        //根据brandId来更新brandName
        UpdateWrapper<CategoryBrandRelationEntity> wrapper = new UpdateWrapper<CategoryBrandRelationEntity>();
        wrapper.eq("brand_id", brandId);
        this.update(relationEntity, wrapper);
    }

    @Override
    public void updateCategory(Long catId, String categoryName) {
//        CategoryBrandRelationEntity relationEntity = new CategoryBrandRelationEntity();
//        relationEntity.setBrandId(catId);
//        relationEntity.setCatelogName(categoryName);
//
//        //根据brandId来更新brandName
//        UpdateWrapper<CategoryBrandRelationEntity> wrapper = new UpdateWrapper<CategoryBrandRelationEntity>();
//        wrapper.eq("brand_id", catId);
//        this.update(relationEntity, wrapper);
        baseMapper.updateCategory(catId, categoryName);
    }

    /**
     * 获取分类下面所有的商品品牌
     *
     * @param catId
     * @return
     */
    @Override
    public List<BrandEntity> getRelatedBrands(Long catId) {
        List<CategoryBrandRelationEntity> relationEntities = this.list(new QueryWrapper<CategoryBrandRelationEntity>()
                .eq("catelog_id", catId));
        List<Long> brandIds = relationEntities.stream()
                .map(CategoryBrandRelationEntity::getBrandId).collect(Collectors.toList());
        List<BrandEntity> brandEntities = brandDao.selectBatchIds(brandIds);
        return brandEntities;
    }

}