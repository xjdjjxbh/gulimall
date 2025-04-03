package com.atguigu.gulimall.thirdparty.controller;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
public class SmsSendController {

    @Autowired
    private SmsComponent smsComponent;

    /**
     * 提供给别的服务调用
     *
     * @param phoneNum 电话号码
     * @param code     要发送的验证码
     * @return
     */
    @GetMapping("sendCode")
    public R sendCode(@RequestParam("phoneNum") String phoneNum, @RequestParam("code") String code) {
        smsComponent.sendCode(phoneNum, code);
        return R.ok();
    }
}
