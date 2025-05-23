/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.atguigu.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 返回数据
 *
 * @author Mark sunlightcs@gmail.com
 */
public class R extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public R setData(Object data) {
        this.put("data", data);
        return this;
    }

    // 利用fastjson进行反序列化
    public <T> T getData(String key, TypeReference<T> typeReference) {
        // 默认是map
        Object data = get(key);
        String jsonString = JSON.toJSONString(data);
        return JSON.parseObject(jsonString, typeReference);
    }

    //传入的参数是我们想要转换的目标类型
    public <T> T getData(TypeReference<T> typeReference) {
        //把取出来的对象先转为json字符串，然后再把这个字符串转为我们需要的数据类型
        Object o = this.get("data");
        //远程调用之后，这里get出来的Object类型其实是map类型，我们先把这个map转为jsonString，然后转为对象
        String jsonString = JSON.toJSONString(o);
        T t = JSON.parseObject(jsonString, typeReference);
        return t;
    }

    public R() {
        put("code", 0);
        put("msg", "success");
    }

    public static R error() {
        return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "未知异常，请联系管理员");
    }

    public static R error(String msg) {
        return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, msg);
    }

    public static R error(int code, String msg) {
        R r = new R();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    public static R ok(String msg) {
        R r = new R();
        r.put("msg", msg);
        return r;
    }

    public static R ok(Map<String, Object> map) {
        R r = new R();
        r.putAll(map);
        return r;
    }

    public static R ok() {
        return new R();
    }

    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    /**
     * 获取返回对象R的状态码
     * 因为R继承了HashMap,所以R其实是一个map，因此可以使用get方法来获取里面的键
     *
     * @return
     */
    public Integer getCode() {
        return (Integer) this.get("code");
    }
}
