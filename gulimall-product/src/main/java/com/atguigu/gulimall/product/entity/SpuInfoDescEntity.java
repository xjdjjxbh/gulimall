package com.atguigu.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * spu信息介绍
 * 
 * @author liuchong
 * @email sunlightcs@gmail.com
 * @date 2025-01-22 20:07:13
 */
@Data
@TableName("pms_spu_info_desc")
public class SpuInfoDescEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 商品id
	 *
	 * MyBatis-Plus 默认认为主键是自增的，所以 不会在 INSERT 语句中手动插入 spu_id
	 * 但是在设计这张表的时候spu_id虽然是主键，但没有设计为自增的，所以就导致执行语句的时候
	 * mysql并不会为insert语句加上自增主键spu_id，从而执行语句的时候因缺少主键而报错
	 *
	 * 在这里加上type = IdType.INPUT就是告诉Mybatis-plus，主键是我们手动输入的，这个时候
	 * mybatis-plus就会给我们加上主键字段了
	 */
	@TableId(type = IdType.INPUT)
	private Long spuId;
	/**
	 * 商品介绍
	 */
	private String decript;

}
