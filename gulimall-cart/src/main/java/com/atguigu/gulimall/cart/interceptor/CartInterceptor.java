package com.atguigu.gulimall.cart.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.constant.CartConstant;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

public class CartInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfoTo userInfoTo = new UserInfoTo();

        //从请求头里面获取session,然后根据sessionId去redis里面获取用户信息，并将其类型转换为MemberResponseVo
        HttpSession session = request.getSession();
        MemberResponseVo member = (MemberResponseVo) session.getAttribute(AuthServerConstant.LOGIN_USER);


        if (member != null) {
            userInfoTo.setUserId(member.getId());
        }


        //从cookid里面获取用户的user-key
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String cookieName = cookie.getName();
                if (cookieName.equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
                    userInfoTo.setUserKey(cookie.getValue());
                    //设置用户已经变为了临时用户
                    userInfoTo.setTempUser(true);
                }
            }
        }


        //如果用户头信息里面没有user-key，则代表用户是游客，这时给用户分配一个user-key

        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }

        threadLocal.set(userInfoTo);

        //这个拦截器只是判断用户是否登陆了，如果登陆了，就会保存用户信息到threadLocal。它会放行所有请求
        return true;
    }


    /**
     * 业务执行完后，分配临时用户，让浏览器保存
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

        /*在业务执行完了之后，如果用户没有user-key,那么将我们创建出来的user-key以cookie的方式返回给用户，让用户保存user-key
         */
        UserInfoTo userInfoTo = threadLocal.get();
        if (userInfoTo.isTempUser()) {
            //如果用户已经是临时用户，则代表用户已经有cookie了，那么可以直接返回
            return;
        }

        Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
        cookie.setDomain("gulimall.com");
        cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);    //设置cookie的过期时间为一个月
        response.addCookie(cookie);
    }
}
