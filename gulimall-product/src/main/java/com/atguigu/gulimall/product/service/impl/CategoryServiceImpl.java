package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalogs2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {


    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private CacheAutoConfiguration cacheAutoConfiguration;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 查出所有分类以及子分类，以树形结构组装起来
     */
    @Override
    public List<CategoryEntity> listWithTree() {
        // 1. 一次性查找所有分类，避免多次查询数据库
        List<CategoryEntity> allCategories = baseMapper.selectList(null);

        // 2. 查找所有的一级分类（parentCid == 0）
        List<CategoryEntity> tree = allCategories.stream().filter(categoryEntity -> categoryEntity.getParentCid() == 0)   //筛选出每个根分类
                .map((entity) -> {
                    entity.setChildren(getChildren(entity, allCategories));    //设置每个根分类的子分类
                    return entity;
                })
                .sorted(Comparator.comparingInt(CategoryEntity::getSort))    //对筛选出来的分类进行排序
                .collect(Collectors.toList());

        return tree;

    }

    /**
     * 批量删除菜单
     *
     * @param catIds
     */
    @Override
    public void removeMenuByIds(List<Long> catIds) {
        //TODO 检查当前菜单是否被别的地方引用
        baseMapper.deleteBatchIds(catIds);
    }

    /**
     * 递归查找子分类
     */
    private List<CategoryEntity> getChildren(CategoryEntity parent, List<CategoryEntity> allCategories) {
        List<CategoryEntity> children = allCategories.stream()
                .filter(entity -> entity.getParentCid().equals(parent.getCatId()))
                .map(entity -> {
                    entity.setChildren(getChildren(entity, allCategories));    //使用递归的方法继续找当前子分类的子分类
                    return entity;
                })
                .sorted(Comparator.comparingInt(CategoryEntity::getSort))
                .collect(Collectors.toList());

        return children;
    }

    /**
     * 根据catelogId找到完整路径
     * 【父路径/子路径/孙子路径】
     *
     * @param catelogId
     * @return
     */
    public Long[] findCatelogPath(Long catelogId) {
        CategoryEntity categoryEntity = this.getById(catelogId);
        List<Long> path = new ArrayList<>();
        path.add(categoryEntity.getCatId());
        while (categoryEntity.getParentCid() != 0) {
            categoryEntity = this.getById(categoryEntity.getParentCid());
            path.add(categoryEntity.getCatId());
        }
        Collections.reverse(path);
        return path.toArray(new Long[0]);
    }

    /**
     * 级联更新所有关联的数据
     *
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);

        //分类表在修改了自己的数据之后还要修改与之关联的表的数据，因为有关联表通过冗余设计使用了分类表里面的字段
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

    }

    /**
     * 查询所有的1级分类
     *
     * @return
     */

    @Cacheable(value = "category",key = "'level1Category'")     //将缓存的数据放到category分区
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        System.out.println("getLevel1Categories");
        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("cat_level", 1);
        List<CategoryEntity> list = this.list(queryWrapper);
        return list;
    }


    /**
     * 查出所有分类
     *
     * @return
     */
    @Override
    public Map<String, List<Catalogs2Vo>> getCatelogJson() {

        /*
         * 空结果缓存：解决缓存穿透问题
         *
         * 加随机时间的过期时间：解决缓存雪崩
         *
         * 加锁：解决缓存击透
         */


        //从缓存里面查询数据
        String categoryJson = stringRedisTemplate.opsForValue().get("categoryJson");

        //如果没有从缓存里面查到数据
        if (categoryJson == null) {
            //从数据库里面查询数据并且把查到的数据放到缓存里面去,获取数据的时候这里使用了加锁，即只能单个线程地获取
            Map<String, List<Catalogs2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithRedissonLock();
            categoryJson = JSON.toJSONString(catelogJsonFromDb);
            stringRedisTemplate.opsForValue().set("categoryJson", categoryJson, 1, TimeUnit.DAYS);
        }

        //将json类型的数据转换为我们想要的数据类型，转换的时候使用TypeReference来指定我们想要转换为什么数据类型
        return JSON.parseObject(categoryJson, new TypeReference<Map<String, List<Catalogs2Vo>>>() {
        });
    }

    public Map<String, List<Catalogs2Vo>> getCatelogJsonFromDbWithRedissonLock() {
        Map<String, List<Catalogs2Vo>> catelogJsonFromDb = null;
        RLock lock = redissonClient.getLock("catelogJson-lock");
        lock.lock();
        try {
            catelogJsonFromDb = getCatelogJsonFromDb();
        } finally {
            lock.unlock();
        }
        return catelogJsonFromDb;

    }


    /**
     * 查出所有分类
     *
     * @return
     */
    public Map<String, List<Catalogs2Vo>> getCatelogJsonFromDb() {

        /*
         * 下面是多次查询数据库，然后对每次的查询进行封装，但是这种效率很低，现在的优化是一次性把所有的数据都查询出来，然后在内存中对这些数据进行封装，
         */

        List<CategoryEntity> allCategories = this.list();


        //1  查出一级分类
//        List<CategoryEntity> level1Categories = getLevel1Categories();
        List<CategoryEntity> level1Categories = getParentsByCid(allCategories, 0L);

        //2 封装数据
        Map<String, List<Catalogs2Vo>> parentCid = level1Categories.stream()
                .collect(Collectors.toMap(
                        key -> key.getCatId().toString()
                        ,
                        v -> {
                            List<CategoryEntity> category2Entities = getParentsByCid(allCategories, v.getCatId());

                            List<Catalogs2Vo> catalogs2Vos = null;
                            if (category2Entities != null) {
                                catalogs2Vos = category2Entities.stream().map(item -> {
                                            Catalogs2Vo catalogs2Vo = new Catalogs2Vo();
                                            catalogs2Vo.setId(item.getCatId().toString());
                                            catalogs2Vo.setName(item.getName());
                                            catalogs2Vo.setCatalog1Id(v.getCatId().toString());

                                            List<CategoryEntity> category3Entities = getParentsByCid(allCategories, item.getCatId());
                                            if (category3Entities != null) {
                                                List<Catalogs2Vo.Category3Vo> category3Vos = category3Entities.stream().map(item2 -> {
                                                    Catalogs2Vo.Category3Vo category3Vo = new Catalogs2Vo.Category3Vo();
                                                    category3Vo.setId(item2.getCatId().toString());
                                                    category3Vo.setName(item2.getName());
                                                    category3Vo.setCatalog2Id(item.getCatId().toString());
                                                    return category3Vo;
                                                }).collect(Collectors.toList());
                                                catalogs2Vo.setCatalog3List(category3Vos);
                                            }

                                            return catalogs2Vo;
                                        }
                                ).collect(Collectors.toList());
                            }
                            return catalogs2Vos;
                        }));

        return parentCid;
    }

    private List<CategoryEntity> getParentsByCid(List<CategoryEntity> allCategories, Long parentCid) {
        return allCategories.stream().filter(entity -> entity.getParentCid().equals(parentCid)).collect(Collectors.toList());
    }
}

