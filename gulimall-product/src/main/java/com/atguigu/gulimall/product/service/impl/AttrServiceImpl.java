package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.constant.ProductConstant;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {


    @Autowired
    private AttrGroupDao attrGroupDao;

    @Autowired
    private CategoryDao categoryDao;


    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attrVo) {
        //先保存基本信息
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attrVo, attrEntity);
        this.save(attrEntity);

        //如果是销售属性，就不用保存分组关联关系
        if (attrVo.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode()
                || attrVo.getAttrGroupId() == null) {
            return;
        }
        //再保存关联关系（基本信息表里面没有分组id字段，要在关联关系表里面保存分组id字段）
        AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();

        //设置属性id，刚才新增属性成功了之后就会得到回显的属性id
        relationEntity.setAttrId(attrEntity.getAttrId());
        relationEntity.setAttrGroupId(attrVo.getAttrGroupId());    //设置属性分组id
        attrAttrgroupRelationDao.insert(relationEntity);

    }

    /**
     * 获取分类规格参数
     *
     * @param params
     * @param catelogId
     * @param type
     * @return
     */
    @Override
    public PageUtils queryBaseAttr(Map<String, Object> params, Long catelogId, String type) {


        String key = (String) params.get("key");    //获取查询关键字

        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("attr_type", "base".equals(type) ?
                ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() :
                ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode()
        );

        //如果分类id为0，则查询所有的分类
        if (catelogId != 0) {
            queryWrapper.eq("catelog_id", catelogId);
        }

        if (StringUtils.isNotEmpty(key)) {
            /*由于前面有可能已经拼装了一个catelog_id条件，而下面的这两个条件是一个整体，所以要使用and（）来
              将他们当做一个整体(在sql中的表现就是使用括号把这两个判断给括起来了)
             */
            queryWrapper.and(wrapper -> wrapper.eq("attr_id", key).or().like("attr_name", key));
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );

        PageUtils pageUtils = new PageUtils(page);

        List<AttrRespVo> attrRespVoList = page.getRecords().stream().map(attrEntity -> {
            //这里创建的是新的对象
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);

            //设置分类名
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }

            QueryWrapper<AttrAttrgroupRelationEntity> wrapper = new QueryWrapper<AttrAttrgroupRelationEntity>()
                    .eq("attr_id", attrEntity.getAttrId());
            AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(wrapper);

            if (relationEntity != null) {
                Long attrGroupId = relationEntity.getAttrGroupId();

                if ("base".equals(type) && attrGroupId != null) {     //只有销售属性才有属性分组信息
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrGroupId);
                    String attrGroupName = attrGroupEntity.getAttrGroupName(); //获取属性分组名
                    attrRespVo.setGroupName(attrGroupName);
                }
            }

            return attrRespVo;     //每创建一个对象就将其返回
        }).collect(Collectors.toList());    //最后收集我们刚才创建的那些对象，并将他们封装为一个列表

        pageUtils.setList(attrRespVoList);     //把创建出来的新的列表对象设置为pageUtils里面的list
        return pageUtils;

/*       return pageUtils.setList(attrRespVoList);
            不可以用这种写法，因为setList的返回值是void类型
 */

    }

    /**
     * 获取属性详情
     *
     * @param attrId
     * @return
     */
    @Override
    public AttrRespVo getDetail(Long attrId) {
        AttrEntity attrEntity = this.getById(attrId);
        AttrRespVo attrRespVo = new AttrRespVo();
        BeanUtils.copyProperties(attrEntity, attrRespVo);

        //设置属性分组id(只有基本属性才需要查询分组信息)
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao
                    .selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if (relationEntity != null) {
                attrRespVo.setAttrGroupId(relationEntity.getAttrGroupId());
            }
        }


        //设置分类完整路径
        Long catelogId = attrEntity.getCatelogId();
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if (categoryEntity == null) {
            return attrRespVo;
        }

        attrRespVo.setCatelogName(categoryEntity.getName());
        ArrayList<Long> list = new ArrayList<>();
        list.add(catelogId);

        while (categoryEntity.getParentCid() != 0) {
            categoryEntity = categoryDao.selectById(categoryEntity.getParentCid());
            list.add(categoryEntity.getCatId());
        }

        Collections.reverse(list);
        Long[] path = list.toArray(new Long[0]);
        attrRespVo.setCatelogPath(path);

        return attrRespVo;
    }

    /**
     * 修改属性详细信息
     *
     * @param attrVo
     */
    @Transactional
    @Override
    public void updateDetail(AttrVo attrVo) {
        //修改基本信息表
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attrVo, attrEntity);
        this.updateById(attrEntity);

        //修改分组关联关系表
        if (attrVo.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attrVo.getAttrGroupId());
            relationEntity.setAttrId(attrVo.getAttrId());

            Integer count = attrAttrgroupRelationDao
                    .selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
            if (count > 0) {
                attrAttrgroupRelationDao.update(
                        relationEntity,
                        new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId())
                );
                return;
            }

            attrAttrgroupRelationDao.insert(relationEntity);
        }


    }

    /**
     * 根据分组id查找所有的属性
     *
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelations(Long attrgroupId) {

        List<AttrAttrgroupRelationEntity> relationEntityList = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>()
                .eq("attr_group_id", attrgroupId));

        List<Long> attrIdList = relationEntityList.stream()
                .map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());

        return attrIdList.isEmpty() ?
                Collections.emptyList() : (List<AttrEntity>) this.listByIds(attrIdList);

    }


    /**
     * 删除属性与分组的关联关系
     *
     * @param attrGroupRelationVos
     * @return
     */
    @Override
    public void deleteRelations(AttrGroupRelationVo[] attrGroupRelationVos) {
//        这种方式不好，因为会发送多次删除请求
//        Arrays.stream(attrGroupRelationVos).forEach(attrGroupRelationVo -> {
//            attrAttrgroupRelationDao.delete(
//                    new UpdateWrapper<AttrAttrgroupRelationEntity>()
//                            .eq("attr_id", attrGroupRelationVo.getAttrId())
//                            .eq("attr_group_id", attrGroupRelationVo.getAttrGroupId())
//            );
//        });

        List<AttrAttrgroupRelationEntity> relationEntityList = Arrays.stream(attrGroupRelationVos).map(
                attrGroupRelationVo -> {
                    AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
                    BeanUtils.copyProperties(attrGroupRelationVo, relationEntity);
                    return relationEntity;
                }
        ).collect(Collectors.toList());
        attrAttrgroupRelationDao.deleteBatchRelations(relationEntityList);

    }

    /**
     * 获取属性分组没有关联的其他属性
     *
     * @param attrgroupId
     * @param params
     * @return
     */
    @Override
    public PageUtils queryNotRelationed(Long attrgroupId, Map<String, Object> params) {
        //1 当前分组只能关联到自己所属分类里面的属性
        //1.1 找到当前分组所属的分类
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();

        //2 当前分组只能关联别的分组里面没有被引用的属性
        //2.1 找到当前分类里面的所有分组id
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>()
                        .eq("catelog_id", catelogId)
//                .ne("attr_group_id", attrgroupId)
        );

        List<Long> attrGroupIds = attrGroupEntities.stream()
                .map(AttrGroupEntity::getAttrGroupId).collect(Collectors.toList());

        //2.2  查询这些分组id所对应分组的详细信息
        //2.2.1 找出这些分组id在关联表里面所对应的所有属性id(既然已经在关联表里面了，就表示这些属性已经被关联起来了)
        QueryWrapper<AttrAttrgroupRelationEntity> relationEntityQueryWrapper = new QueryWrapper<>();
        if (!attrGroupIds.isEmpty()) {
            relationEntityQueryWrapper.in("attr_group_id", attrGroupIds);
        }

        List<AttrAttrgroupRelationEntity> relationEntityList = attrAttrgroupRelationDao
                .selectList(relationEntityQueryWrapper);

        //从关联表里面找到了这些属性的id，就表示这些属性已经被关联起来了
        List<Long> attrIds = relationEntityList.stream()
                .map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());

        //2.2.2  找出这些属性id所对应属性的所有详细信息
        String key = params.get("key").toString();

        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>()
                .eq("catelog_id", catelogId)     //只能查询本分类里面的
                .eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());

        //如果其它分组里面没有关联属性就不要添加这个条件了
        if (!attrIds.isEmpty()) {
            //排除已经被关联起来了的属性
            queryWrapper.notIn("attr_id", attrIds);
        }

        if (StringUtils.isNotEmpty(key)) {
            queryWrapper.and(wrapper -> wrapper.eq("attr_id", key).or().like("attr_name", key));
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 查出能够被搜索的属性
     * @param attrIds
     * @return
     */
    @Override
    public List<Long> selectSearchAttrs(List<Long> attrIds) {
        return baseMapper.selectSearchAttrIds(attrIds);
    }


}