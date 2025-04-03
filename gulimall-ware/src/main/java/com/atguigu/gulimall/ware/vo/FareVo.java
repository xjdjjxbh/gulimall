package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单地址
 */
@Data
public class FareVo {

    private MemberAddressVo memberAddressVo;

    private BigDecimal fare;
}
