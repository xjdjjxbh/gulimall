package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;

public interface MallSearchService {

    /**
     *
     * @param searchParam   检索的所有参数
     * @return     返回的结果
     */
    SearchResult search(SearchParam searchParam);
}
