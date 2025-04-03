package com.atguigu.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class GuliFeignConfiguration {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            public void apply(RequestTemplate requestTemplate) {
                //1.RequestContextHolder拿到request请求(通过threadLocal)
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {

                    HttpServletRequest request = attributes.getRequest();   //获取旧请求
                    String cookie = request.getHeader("Cookie");
                    if (cookie != null) {
                        requestTemplate.header("Cookie", cookie);   //将就请求中的Cookie放到新请求中去
                    }
                }
            }
        };
    }
}
