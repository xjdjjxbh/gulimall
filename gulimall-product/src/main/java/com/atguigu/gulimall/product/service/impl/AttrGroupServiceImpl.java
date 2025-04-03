package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.service.AttrAttrgroupRelationService;
import com.atguigu.gulimall.product.service.AttrGroupService;
import com.atguigu.gulimall.product.service.ProductAttrValueService;
import com.atguigu.gulimall.product.service.SpuInfoService;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    private final AttrGroupDao attrGroupDao;
    private final AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    private SpuInfoService spuInfoService;

    @Autowired
    private ProductAttrValueService productAttrValueService;
    @Autowired
    private AttrGroupService attrGroupService;
    @Autowired
    private AttrAttrgroupRelationService attrAttrgroupRelationService;


    public AttrGroupServiceImpl(AttrGroupDao attrGroupDao, AttrAttrgroupRelationDao attrAttrgroupRelationDao) {
        this.attrGroupDao = attrGroupDao;
        this.attrAttrgroupRelationDao = attrAttrgroupRelationDao;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    //属性分组三级分类查询
    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {

        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();

        if (StringUtils.isNotEmpty(key)) {
            wrapper.and((obj) -> obj.like("attr_group_name", key).or().eq("attr_group_id", key));
        }

        //如果前端传递的catelogId这个参数为0，就表示查询所有
        if (catelogId == 0) {
            IPage<AttrGroupEntity> page = this.page(     //查询方法
                    new Query<AttrGroupEntity>().getPage(params),      //设置分页信息
                    wrapper
            );
            return new PageUtils(page);
        }


        IPage<AttrGroupEntity> page = this.page(     //查询方法
                new Query<AttrGroupEntity>().getPage(params),      //设置分页信息
                wrapper.eq("catelog_id", catelogId)
        );
        return new PageUtils(page);
    }

    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        ArrayList<SpuItemAttrGroupVo> list = new ArrayList<>();

        //查询当前spu对应的所有分组信息，以及当前分组下所有的属性对应的值

        AttrGroupDao baseMapper = this.getBaseMapper();
        list = baseMapper.getAttrGroupWithAttrsBySpuId(spuId, catalogId);

        return list;
    }


}