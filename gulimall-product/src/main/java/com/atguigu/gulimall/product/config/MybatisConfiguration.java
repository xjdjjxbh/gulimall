package com.atguigu.gulimall.product.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement    //开启事务
@MapperScan("com.atguigu.gulimall.product.dao")
public class MybatisConfiguration {
    /**
     * 添加分页插件
     */
    @Bean
    public PaginationInterceptor mybatisPlusInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        paginationInterceptor.setOverflow(true);    //如果请求的页面大于最后一页就跳转到第一页
        paginationInterceptor.setLimit(100);     //设置每页最大数量
        return paginationInterceptor;
    }
}
