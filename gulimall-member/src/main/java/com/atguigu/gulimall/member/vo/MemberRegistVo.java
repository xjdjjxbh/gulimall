package com.atguigu.gulimall.member.vo;

import lombok.Data;


/*
这里不用数据校验，是因为在认证微服务里面已经校验过了，这里可以直接存储用户信息
 */
@Data
public class MemberRegistVo {

    private String userName;

    private String password;

    private String phone;
}
