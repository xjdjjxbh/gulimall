package com.atguigu.gulimall.search.service;

import com.atguigu.common.to.SkuEsModel;

import java.io.IOException;
import java.util.List;


public interface ProductSaveService {
    boolean productStatueUp(List<SkuEsModel> skuEsModels) throws IOException;
}
