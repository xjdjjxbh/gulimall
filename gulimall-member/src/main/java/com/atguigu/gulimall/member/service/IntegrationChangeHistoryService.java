package com.atguigu.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.member.entity.IntegrationChangeHistoryEntity;

import java.util.Map;

/**
 * 积分变化历史记录
 *
 * @author liuchong
 * @email sunlightcs@gmail.com
 * @date 2025-01-22 22:40:03
 */
public interface IntegrationChangeHistoryService extends IService<IntegrationChangeHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

