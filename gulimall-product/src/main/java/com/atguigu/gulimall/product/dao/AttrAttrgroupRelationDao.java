package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性&属性分组关联
 * 
 * @author liuchong
 * @email sunlightcs@gmail.com
 * @date 2025-01-22 20:07:13
 */
@Mapper
public interface AttrAttrgroupRelationDao extends BaseMapper<AttrAttrgroupRelationEntity> {

    void deleteBatchRelations(@Param("relationEntityList") List<AttrAttrgroupRelationEntity> relationEntityList);

    void insertRelations(@Param("relations") AttrGroupRelationVo[] attrGroupRelationVos);
}
