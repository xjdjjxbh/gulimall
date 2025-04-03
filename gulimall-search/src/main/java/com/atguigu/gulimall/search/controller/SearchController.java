package com.atguigu.gulimall.search.controller;

import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/*
@Controller 是 Spring MVC 的 控制器（Controller）注解，表示这个类是一个 处理 HTTP 请求的控制器，默认情况下方法返回的是 视图（页面）。
 */
@Controller
public class SearchController {

    @Autowired
    private MallSearchService searchService;

    /*
        如果方法的参数是一个对象（比如 SearchParam），Spring MVC 会根据请求参数的 key 名字，自动填充对象的对应字段。这种情况下不用使用@RequestParam注解

        public String listPage(@RequestParam("keyword") String keyword,
                       @RequestParam("page") int page,
                       @RequestParam("size") int size,
                       Model model) {
       如果接收参数的时候不是使用对象接收，而是使用属性接收，那么就必须使用@RequestParam注解
     */

    @GetMapping(value = "/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request) {

        param.set_queryString(request.getQueryString());

        //1、根据传递来的页面的查询参数，去es中检索商品
        SearchResult result = searchService.search(param);

        model.addAttribute("result", result);

        return "list";
    }

}
