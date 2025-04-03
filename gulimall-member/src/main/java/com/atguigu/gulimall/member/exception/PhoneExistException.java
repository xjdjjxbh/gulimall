package com.atguigu.gulimall.member.exception;

public class PhoneExistException extends RuntimeException {
    public PhoneExistException() {
        //在本类的构造方法里面调用父类的无参构造方法
        super("手机号已存在");
    }
}
