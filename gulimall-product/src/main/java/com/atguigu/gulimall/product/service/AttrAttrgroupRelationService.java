package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;

import java.util.List;
import java.util.Map;

/**
 * 属性&属性分组关联
 *
 * @author liuchong
 * @email sunlightcs@gmail.com
 * @date 2025-01-22 20:07:13
 */
public interface AttrAttrgroupRelationService extends IService<AttrAttrgroupRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addRelation(AttrGroupRelationVo[] attrGroupRelationVos);

    /**
     * 获取分类下所有分组&关联属性
     * @param catelogId   分类id
     * @return
     */
    List<AttrGroupWithAttrsVo> getGroupsWithAttrs(Long catelogId);
}

