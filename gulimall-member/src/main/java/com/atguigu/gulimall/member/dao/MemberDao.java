package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author liuchong
 * @email sunlightcs@gmail.com
 * @date 2025-01-22 22:40:03
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
