package com.atguigu.gulimall.ssoclient.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;

@Controller
public class HelloController {

    @Value("${sso.server.url}")
    private String ssoServerUrl;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/boss")
    public String employees(@RequestParam(value = "token",required = false) String token, Model model, HttpSession session) {

        /*
         * 如果token不为空则代表这次的请求可能是登录成功之后的请求
         */
        if (token != null) {
            //如果传递过来的token是真实有效的token
            String redisToken = stringRedisTemplate.opsForValue().get(token);
            if (redisToken != null) {

                session.setAttribute("loginUser", "zhangsan");

            }
        }

//        从session里面取出用户信息，如果无法取出，则代表用户没有登陆，那么我们就跳转到登录页面
        Object user = session.getAttribute("loginUser");
        if (user != null) {
            ArrayList<String> list = new ArrayList<>();
            list.add("张三");
            list.add("李四");
            list.add("王五");
            list.add("赵六");
            model.addAttribute("emps", list);
            return "list";
        }

        /*
        用户没有登录，跳转到认证服务器，当用户在认证服务器输入了账号和密码之后，我们又使用
        ?redirect_url=http://client1.com:8081/emps 这个参数来跳转回来到客户端（访问我们想要的页面）
         */
        return "redirect:" + ssoServerUrl + "?redirect_url=http://client2.com:8082/boss";
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }
}
