package com.atguigu.gulimall.product.feign;

import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.to.SpuBoundTo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    /**
     * 如果有一个方法调用了这个接口里面的这个方法，调用者会传过来一个对象SpuBoundTo
     * 而这个feign接口是用来进行远程调用的，它在远程调用的时候需要把调用者传过来的对象变为json数据之后
     * 才能进行数据传输，所以这里需要加上@RequestBody注解来将对象转为json对象
     * 变为json对象之后，会把这个json对象放在请求体位置，再发送远程调用请求，所以远程服务也需要
     * 使用@RequestBody注解来将发送过去的json对象转为java实体类
     *
     * 这个注解既可以将对象转为json,又可以将json转为对象
     *
     * @param spuBoundTo  这个形参列表并不是必须和远程服务的形参列表类型相同，只要里面有相同的字段
     *                    就可以正常传递数据
     * @return
     */
    @PostMapping("/coupon/spubounds/save")
    R saveCouponBounds(@RequestBody SpuBoundTo spuBoundTo);

    @PostMapping("/coupon/skufullreduction/saveInfo")
    R saveSkuRedution(@RequestBody SkuReductionTo skuReductionTo);
}
