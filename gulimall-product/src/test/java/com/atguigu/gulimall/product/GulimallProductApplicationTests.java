package com.atguigu.gulimall.product;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.service.impl.SkuInfoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;

@SpringBootTest
@Slf4j
class GulimallProductApplicationTests {

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private AttrGroupService attrGroupService;
    @Autowired
    private SkuInfoServiceImpl skuInfoService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

//    @Autowired
//    private OSS ossClient;

    @Test
    void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setName("华为");
        brandEntity.setLogo("叶子");
        brandEntity.setDescript("华为手机");
        brandService.save(brandEntity);
    }

    @Test
    void test2() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setBrandId(3L);
        brandEntity.setName("华为爱国手机");
        brandService.updateById(brandEntity);
    }

    @Test
    void test5() {
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        log.info("完整路径:{}", Arrays.toString(catelogPath));
    }


    @Test
    void test6() {

//        HashMap<String, Object> map = new HashMap<>();

        SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
        skuInfoEntity.setSkuName("早上好");
        R r = R.ok().put("spuInfo", skuInfoEntity);
        SkuInfoEntity spuinfo = (SkuInfoEntity) (r.get("spuInfo"));
        System.out.println(spuinfo.getSkuName());


//        map.put("skuInfo", skuInfoEntity);
//        SkuInfoEntity skuInfo = (SkuInfoEntity)map.get("skuInfo");
//        System.out.println(skuInfo);
    }


    @Test
    void test7() {
        stringRedisTemplate.opsForValue().set("hello", "world");
        System.out.println(stringRedisTemplate.opsForValue().get("hello"));
    }

    @Test
    void test8() {
        System.out.println(attrGroupService.getAttrGroupWithAttrsBySpuId(7L, 225L));

    }

    @Test
    void test9() {
        System.out.println(skuSaleAttrValueService.getSaleAttrsBySpuId(7L));

    }
}
