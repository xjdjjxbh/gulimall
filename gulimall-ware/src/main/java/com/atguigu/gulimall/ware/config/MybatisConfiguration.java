package com.atguigu.gulimall.ware.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement    //开启事务     跟数据库有关的配置就卸载这个配置类里面，不要在启动类上面加
@MapperScan("com.atguigu.gulimall.ware.dao")
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
