package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Autowired
    private ThirdPartFeignService thirdPartFeignService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MemberFeignService memberFeignService;

    @ResponseBody
    @GetMapping("/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone) {

        /*防止用户短时间内多次发送验证码
        从redis获取该用户上请求的验证码
         */
        String preCode = stringRedisTemplate.opsForValue()
                .get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);

        if (preCode != null) {
            long l = Long.parseLong(preCode.split("_")[1]);
            if (System.currentTimeMillis() - l < 60000) {
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }


        String returnCode = UUID.randomUUID().toString().substring(0, 5);
        String code = returnCode + "_" + System.currentTimeMillis();
        thirdPartFeignService.sendCode(phone, returnCode);

        /*把验证码存储到redis里面去,并设置一分钟有效
        键是手机号，值是验证码加上验证码有效时间 ,验证码5分钟内有效
         */
        stringRedisTemplate.opsForValue()
                .set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, code, 5, TimeUnit.MINUTES);
        return R.ok().put("data", returnCode);
    }

    /**
     * 重定向携带数据，session原理，将数据放在session中，重定向到下一个页面之后，从session里面取出数据，
     * 然后这个session里面的数据集就会被删掉，这刚就不会导致表单的重复提交
     * <p>
     * //todo 分布式下的session问题
     *
     * @param userRegisVo
     * @param bindingResult
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo userRegisVo, BindingResult bindingResult, RedirectAttributes redirectAttributes, HttpSession session) {
        //校验失败转发到注册页
        if (bindingResult.hasErrors()) {
            Map<String, String> collect = bindingResult.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, item -> item.getDefaultMessage()));
            redirectAttributes.addFlashAttribute("errors", collect);
            return "redirect:http://auth.gulimall.com/reg.html";
        }

        //真正注册，调用远程服务进行注册
        String userCode = userRegisVo.getCode();

        //从redis里面取出用户电话号码对应的验证码
        String s = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + userRegisVo.getPhone());
        String trueCode = s.split("_")[0];
        if (!StringUtils.isEmpty(trueCode) && trueCode.equals(userRegisVo.getCode())) {

            //验证成功，删除redis里面保存的验证码
            stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + userRegisVo.getPhone());

            R regist = memberFeignService.regist(userRegisVo);

            //如果向数据库里面添加用户信息失败，则再次返回到注册页
            if (regist.getCode() != 0) {
                HashMap<String, String> errors = new HashMap<>();
                errors.put("msg", regist.getData("msg", new TypeReference<String>() {
                }));
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }

        } else {   //如果用户输入的手机号不存在验证码，或者用户输入的验证码错误
            HashMap<String, String> map = new HashMap<>();
            map.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors", map);

            //用户验证码错误，重定向到注册页面
            return "redirect:http://auth.gulimall.com/reg.html";
        }

        //注册成功之后回到登录页
        return "redirect:http://auth.gulimall.com/login.html";
    }

    @PostMapping("/login")
    public String login(UserLoginVo userLoginVo, RedirectAttributes redirectAttributes,
                        HttpSession session) {
        R login = memberFeignService.login(userLoginVo);
        if (login.getCode() != 0) {
            HashMap<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg", new TypeReference<String>() {
            }));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
        MemberResponseVo data = login.getData("data", new TypeReference<MemberResponseVo>() {
        });
        session.setAttribute(AuthServerConstant.LOGIN_USER, data);
        return "redirect:http://gulimall.com";
    }


}
