package com.atguigu.gulimall.product.service;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author liuchong
 * @email sunlightcs@gmail.com
 * @date 2025-01-22 20:07:13
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attrVo);

    PageUtils queryBaseAttr(Map<String, Object> params, Long catelogId, String attrType);

    AttrRespVo getDetail(Long attrId);

    void updateDetail(AttrVo attrVo);

    /**
     * 获取当前分组关联的所有属性
     */
    List<AttrEntity> getRelations(Long attrgroupId);

    /**
     * 删除属性与分组的关联关系
     *
     * @param attrGroupRelationVos
     * @return
     */
    void deleteRelations(AttrGroupRelationVo[] attrGroupRelationVos);


    /**获取属性分组没有关联的其他属性
     * @param attrgroupId
     * @param params
     * @return
     */
    PageUtils queryNotRelationed(Long attrgroupId, Map<String, Object> params);

    List<Long> selectSearchAttrs(List<Long> attrIds);

    /**
     * 添加属性与分组关联关系
     * @param attrGroupRelationVos
     */

}

