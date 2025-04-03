package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalogs2Vo;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedissonClient redissonClient;

    @GetMapping({"/", "index.html"})
    public String index(Model model) {
        //给model中存放的数据就会放到页面的请求域中，这里是页面的转发，所以就会把数据放到页面

        //todo 查出所有的1级分类数据
        List<CategoryEntity> categoryList = categoryService.getLevel1Categories();

        /*因为spring已经自动配置好了资源的默认前缀templates和html,所以返回的资源路径再就不用加了
        spring会利用视图解析器进行拼串
         */

        model.addAttribute("categorys", categoryList);
        return "index";   //返回页面跳转地址
    }


    @GetMapping({"/index/catalog.json"})
    @ResponseBody
    public Map<String, List<Catalogs2Vo>> getCategorys(Model model) {

        // 查出所有的1级分类数据
        Map<String, List<Catalogs2Vo>> map = categoryService.getCatelogJson();

        return map;
    }

    @GetMapping({"/hello"})
    @ResponseBody
    public String hello() {
        redissonClient.getLock("myLock").lock();
        System.out.println("线程"+Thread.currentThread().getName()+"获取到了锁");
        System.out.println("==============");
        System.out.println("线程"+Thread.currentThread().getName()+"释放了锁");
        redissonClient.getLock("myLock").unlock();
        return "hello";
    }
}
