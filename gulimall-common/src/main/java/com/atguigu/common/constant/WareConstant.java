package com.atguigu.common.constant;

import lombok.Getter;

public class WareConstant {

    /**
     * 采购单状态
     */
    @Getter
    public enum PurchaseStatusEnum {
        CREATED(0, "新建"),
        ASSIGNED(1, "已分配"),
        RECEIVED(2, "已领取"),
        FINISHED(3, "已完成"),
        HASERROR(4, "异常");


        private final int status;
        private final String msg;

        PurchaseStatusEnum(int code, String msg) {
            this.status = code;
            this.msg = msg;
        }

    }


    /**
     * 采购需求状态
     */
    @Getter
    public enum PurchaseDetailEnum {
        CREATED(0, "新建"),
        ASSIGNED(1, "已分配"),
        BUYING(2, "正在采购"),
        FINISHED(3, "已完成"),
        FAILED(4, "采购失败");


        private final int status;
        private final String msg;

        PurchaseDetailEnum(int code, String msg) {
            this.status = code;
            this.msg = msg;
        }

    }
}
