package com.atguigu.gulimall.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {

    /*
    视图映射
    前者是前端请求的资源地址，后者是返回的页面名称
    使用了这个配置方法之后，我们就不用在controller里面使用空方法了
    因为内这些空方法只是为了实现页面跳转，多了的话就会显得很麻烦
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login.html").setViewName("login");
        registry.addViewController("/reg.html").setViewName("reg");
    }
}
