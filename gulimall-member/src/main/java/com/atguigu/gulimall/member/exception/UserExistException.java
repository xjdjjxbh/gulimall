package com.atguigu.gulimall.member.exception;

public class UserExistException extends RuntimeException {
    public UserExistException() {
        super("用户名已存在");
    }
}
