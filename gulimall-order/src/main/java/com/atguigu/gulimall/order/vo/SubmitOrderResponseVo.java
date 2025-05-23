package com.atguigu.gulimall.order.vo;

import com.atguigu.gulimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {

    private OrderEntity orderEntity;

    /**
     * 错误状态码： 0----成功
     */
    private Integer code;
}
