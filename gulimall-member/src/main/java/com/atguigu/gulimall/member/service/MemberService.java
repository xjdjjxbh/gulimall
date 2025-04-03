package com.atguigu.gulimall.member.service;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.vo.SocialUser;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.entity.MemberReceiveAddressEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UserExistException;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 会员
 *
 * @author liuchong
 * @email sunlightcs@gmail.com
 * @date 2025-01-22 22:40:03
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo memberRegistVo);

    /*
    在这两个接口上添加抛出异常，当调用者来调用这个方法的时候，用户可以选择是否对这两个方法抛出的异常进行处理
     */
    void checkPhoneUnique(String phone) throws PhoneExistException;
    void checkUserNameUnique(String username) throws UserExistException;

    MemberEntity login(MemberLoginVo memberLoginVo);

    MemberEntity login(SocialUser socialUser) throws Exception;

    List<MemberReceiveAddressEntity> getAddresses(Long memberId);
}

