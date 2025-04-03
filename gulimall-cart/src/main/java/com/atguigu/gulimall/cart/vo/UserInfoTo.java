package com.atguigu.gulimall.cart.vo;


import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class UserInfoTo {

    private Long userId;
    private String userKey;
    //是否已经是临时用户
    private boolean tempUser = false;
}
