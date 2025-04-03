package com.atguigu.gulimall.product.config;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@EnableConfigurationProperties
@EnableCaching
@Configuration
public class MyCacheConfig {

    //这个方法是要往容器里面放对象，这个方法里面的形参对象都是由spring自动从容器里面获取
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {

        /*
        错误的配置：因为下面第二行和第三行代码执行之后都会返回一个新的配置对象，然后在新的配置对象上面进行配置，而没有改变老的
        但是我却没有接收新的配置对象，从而导致返回的配置仍然是老的配置，相当于什么都没有配置
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
        redisCacheConfiguration.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
        redisCacheConfiguration.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericFastJsonRedisSerializer()));
        return redisCacheConfiguration;
         */

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));


        /*让配置文件中的配置都生效
            CacheProperties 是 Spring Boot 提供的一个封装类，用于读取 application.yml 里的 spring.cache.* 配置。
            @EnableConfigurationProperties 的作用就是让 CacheProperties 这个类的属性自动从 application.yml 里读取，
            否则 cacheProperties 可能是 null，导致配置不会生效。

            如果不加 @EnableConfigurationProperties，Spring Boot 不会自动绑定 CacheProperties，导致 cacheProperties.getRedis() 可能为空。
            CacheProperties 并不是你自己创建的 Bean，而是 Spring Boot 提供的，
            所以必须用 @EnableConfigurationProperties 让 Spring 扫描它，并把 application.yml 里的值绑定到这个类
         */
        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        //将配置文件中所有的配置都生效
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixKeysWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }

        return config;
    }
}
