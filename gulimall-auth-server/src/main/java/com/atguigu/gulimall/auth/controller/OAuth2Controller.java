package com.atguigu.gulimall.auth.controller;

import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.Date;

import static com.atguigu.common.constant.AuthServerConstant.LOGIN_USER;

@Slf4j
@Controller
public class OAuth2Controller {
    private final MemberFeignService memberFeignService;

    @Autowired
    public OAuth2Controller(MemberFeignService memberFeignService) {
        this.memberFeignService = memberFeignService;
    }

    @GetMapping(value = "/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {

        MemberResponseVo data = new MemberResponseVo();
        data.setId(100L);
        data.setNickname("lisi");
        data.setGender(1);
        data.setHeader("https://gulimall-liuchong.oss-cn-beijing.aliyuncs.com/679601ed88d836dc9f3798eaa98b24f1.png");
        data.setCreateTime(new Date());
        data.setSocialUid("1");
        data.setAccessToken("IamAfakeToken");
        data.setExpiresIn(1800);    //设置token在30分钟之后过期
        log.info("登录成功：用户信息：{}", data.toString());

        //1、第一次使用session，命令浏览器保存卡号，JSESSIONID这个cookie
        //以后浏览器访问哪个网站就会带上这个网站的cookie
        //TODO 1、默认发的令牌。当前域（解决子域session共享问题）
        //TODO 2、使用JSON的序列化方式来序列化对象到Redis中
        session.setAttribute(LOGIN_USER, data);

        //2、登录成功跳回首页
        return "redirect:http://gulimall.com";
    }

}
