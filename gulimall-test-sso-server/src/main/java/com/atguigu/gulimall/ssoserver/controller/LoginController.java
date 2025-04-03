package com.atguigu.gulimall.ssoserver.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Controller
public class LoginController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 这个方法是用户没有登录的时候，如果要查看详细信息，就会跳转到这里来进行登录
     *
     * @param redirectUrl
     * @param model
     * @param cookie
     * @return
     */
    @GetMapping("/login.html")
    public String login(@RequestParam("redirect_url") String redirectUrl,
                        Model model, @CookieValue(value = "sso_token", required = false) String cookie) {

        if (cookie != null ) {
            //如果这里已经有了token，就代表用户之前已经登录过了，那么旧知己跳回到详情页面
            return "redirect:" + redirectUrl + "?token=" + cookie;
        }

        /*
        把参数里面的地址传递给前端，前端在填写完了账号和密码之后，点击登录。我们将登录按钮绑定到这个要跳转的redirectUrl
        这样用户点击了登录之后，就可以调回到登录之前想要看的页面了
         */
        model.addAttribute("url", redirectUrl);

        return "login.html";
    }

    @PostMapping("/doLogin")
    public String doLogin(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          @RequestParam("url") String url,
                          HttpServletResponse response) {
        //登陆成功，跳转到我们想要访问的页面
        String token = UUID.randomUUID().toString().replace("-", "");

        stringRedisTemplate.opsForValue().set(token, username);

        if (!username.isEmpty() && !password.isEmpty()) {
            Cookie cookie = new Cookie("sso_token", token);
            response.addCookie(cookie);
            return "redirect:" + url + "?token=" + token;
        }

        //登录失败，跳转到登录页面
        return "login.html";
    }
}
