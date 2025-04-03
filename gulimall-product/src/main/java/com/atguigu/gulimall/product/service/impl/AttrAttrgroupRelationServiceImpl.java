package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.service.AttrAttrgroupRelationService;
import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrAttrgroupRelationService")
public class AttrAttrgroupRelationServiceImpl extends ServiceImpl<AttrAttrgroupRelationDao, AttrAttrgroupRelationEntity> implements AttrAttrgroupRelationService {

    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;
    @Autowired
    private AttrGroupDao attrGroupDao;
    @Autowired
    private AttrDao attrDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrAttrgroupRelationEntity> page = this.page(
                new Query<AttrAttrgroupRelationEntity>().getPage(params),
                new QueryWrapper<AttrAttrgroupRelationEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 添加属性与分组关联关系
     *
     * @param attrGroupRelationVos
     */
    @Override
    public void addRelation(AttrGroupRelationVo[] attrGroupRelationVos) {
        attrAttrgroupRelationDao.insertRelations(attrGroupRelationVos);

    }

    /**
     * 获取分类下所有分组&关联属性
     *
     * @param catelogId 分类id
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> getGroupsWithAttrs(Long catelogId) {
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>()
                .eq("catelog_id", catelogId));

        //获取每个分组下面的所有关联属性
        List<AttrGroupWithAttrsVo> collect = attrGroupEntities.stream().map(groupEntity -> {
            //对于每一个分组，查询出它里面的所有属性
            List<AttrAttrgroupRelationEntity> relationEntityList = attrAttrgroupRelationDao
                    .selectList(new QueryWrapper<AttrAttrgroupRelationEntity>()
                            .eq("attr_group_id", groupEntity.getAttrGroupId()));
            //获取该分组下面所有属性的id列表
            List<Long> attrIds = relationEntityList.stream().map(AttrAttrgroupRelationEntity::getAttrId)
                    .collect(Collectors.toList());

            //查询该分组下面的所有属性
            if (attrIds.isEmpty()) {
                //如果该分组下面没有任何属性，那么就只用封装组信息
                AttrGroupWithAttrsVo attrGroupWithAttrsVo = new AttrGroupWithAttrsVo();
                BeanUtils.copyProperties(groupEntity, attrGroupWithAttrsVo);
                return attrGroupWithAttrsVo;
            }
            QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>()
                    .in("attr_id", attrIds);
            List<AttrEntity> attrEntities = attrDao.selectList(queryWrapper);

            //封装vo
            AttrGroupWithAttrsVo attrGroupWithAttrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(groupEntity, attrGroupWithAttrsVo);
            attrGroupWithAttrsVo.setAttrs(attrEntities);
            return attrGroupWithAttrsVo;
        }).collect(Collectors.toList());

        return collect;

    }

}