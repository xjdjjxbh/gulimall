package com.atguigu.gulimall.member.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.vo.SocialUser;
import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.entity.MemberReceiveAddressEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UserExistException;
import com.atguigu.gulimall.member.service.MemberLevelService;
import com.atguigu.gulimall.member.service.MemberReceiveAddressService;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private MemberLevelService memberLevelService;

    @Autowired
    private MemberReceiveAddressService memberReceiveAddressService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 用户注册
     *
     * @param memberRegistVo
     */
    @Override
    public void regist(MemberRegistVo memberRegistVo) {

        /*
        检查用户名和手机号的唯一性，因为用户是根据用户名或者手机号来登录的，它们不能和其它用户的信息重复了
        因为这个注册微服务是被登录校验微服务调用的，如果注册失败。我们首先需要让本微服务的controller感知到注册失败
        所以当发现有重复的时候，我们直接抛出异常
         */
        checkPhoneUnique(memberRegistVo.getPhone());
        checkUserNameUnique(memberRegistVo.getUserName());


        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setUsername(memberRegistVo.getUserName());
        memberEntity.setNickname(memberRegistVo.getUserName());

        //对密码进行加盐后然后进行md5加密
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(memberRegistVo.getPassword());
        memberEntity.setPassword(encodedPassword);

        memberEntity.setMobile(memberRegistVo.getPhone());

        //查询会员默认等级对应的id
        MemberLevelEntity memberLevelEntity = memberLevelService.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());

        memberEntity.setCreateTime(new Date());
        this.save(memberEntity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        int phoneCount = this.count(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (phoneCount > 0) {
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUserNameUnique(String username) throws UserExistException {
        int userCount = this.count(new QueryWrapper<MemberEntity>().eq("username", username));
        if (userCount > 0) {
            throw new UserExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo memberLoginVo) {
        MemberEntity memberEntity = this.getOne(new QueryWrapper<MemberEntity>()
                .eq("username", memberLoginVo.getLoginAcct())
                .or()
                .eq("mobile", memberLoginVo.getLoginAcct()));
        //如果当前登录用户不存在
        if (memberEntity == null) {
            return null;
        }

        //这里不能对密码加密，然后再去数据库查找账号密码和用户传递的一致的，因为密码每次加密的结果都是不一样的
        String password = memberEntity.getPassword();
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean matches = passwordEncoder.matches(memberLoginVo.getPassword(), password);
        //如果真实密码和用户传过来的密码不一致
        if (!matches) {
            return null;
        }

        return memberEntity;
    }

    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {

        //具有登录和注册逻辑
        String uid = socialUser.getUid();

        //1、判断当前社交用户是否已经登录过系统
        MemberEntity memberEntity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));

        if (memberEntity != null) {
            //这个用户已经注册过
            //更新用户的访问令牌的时间和access_token
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(socialUser.getExpires_in());
            baseMapper.updateById(update);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity;
        } else {
            //2、没有查到当前社交用户对应的记录我们就需要注册一个
            MemberEntity register = new MemberEntity();

            //查询成功
            String name = "lisi";
            String gender = "m";
            String profileImageUrl = "https://gulimall-liuchong.oss-cn-beijing.aliyuncs.com/679601ed88d836dc9f3798eaa98b24f1.png";

            register.setNickname(name);
            register.setGender("m".equals(gender) ? 1 : 0);
            register.setHeader(profileImageUrl);
            register.setCreateTime(new Date());
            register.setSocialUid(socialUser.getUid());
            register.setAccessToken(socialUser.getAccess_token());
            register.setExpiresIn(socialUser.getExpires_in());
            register.setId(100L);


            //把用户信息插入到数据库中
            baseMapper.insert(register);

            return register;
        }
    }

    /**
     * 获取会员的收货地址列表
     *
     * @param memberId
     * @return
     */
    @Override
    public List<MemberReceiveAddressEntity> getAddresses(Long memberId) {
        return memberReceiveAddressService.list(new QueryWrapper<MemberReceiveAddressEntity>()
                .eq("member_id", memberId));
    }

}