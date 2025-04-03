package com.atguigu.gulimall.thirdparty.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@ConfigurationProperties(prefix = "spring.cloud.alicloud.sms")
@Component
public class SmsComponent {
    public void sendCode(String phone, String code) {
        //向手机发送验证码,这里直接输出到控制台
        log.info("电话号码:{},收到的验证码是:{}", phone, code);
    }
}
