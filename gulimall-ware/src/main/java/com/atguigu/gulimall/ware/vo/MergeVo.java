package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class MergeVo {

    /**
     * 采购单Id
     */
    private Long purchaseId;

    /**
     * 采购需求
     */
    private List<Long> items;
}
