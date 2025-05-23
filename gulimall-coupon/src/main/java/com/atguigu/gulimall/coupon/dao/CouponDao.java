package com.atguigu.gulimall.coupon.dao;

import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author liuchong
 * @email sunlightcs@gmail.com
 * @date 2025-01-22 22:28:48
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
