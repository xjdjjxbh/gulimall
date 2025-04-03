package com.atguigu.gulimall.ware.service;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author liuchong
 * @email sunlightcs@gmail.com
 * @date 2025-01-22 23:02:11
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryUnreceivePurchaseList(Map<String, Object> params);


    void mergePurchase(MergeVo mergeVo);

    void receivePurchase(List<Long> purchaseIds);

    void finishPurchase(PurchaseDoneVo purchaseDoneVo);
}

