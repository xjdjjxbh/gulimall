package com.atguigu.common.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 传输对象，存储到es的数据
 */
@Data
public class SkuEsModel {
    private Long skuId;
    private Long spuId;
    private String skuTitle;
    private BigDecimal skuPrice;
    private String skuImg;
    private Long saleCount;
    /**
     * 是否有库存
     */
    private Boolean hasStock;
    /**
     * 热度
     */
    private Long hotScore;
    private Long brandId;
    private Long catalogId;
    private String brandName;
    private String brandImg;
    private String catalogName;
    private List<Attrs> attrs;


    /*
        由于 Attrs 是 static 的，它不依赖 SkuEsModel 的实例，因此可以直接通过 SkuEsModel.Attrs 进行实例化
        不需要 先创建 SkuEsModel 的对象,只要是创建对象，就需要使用new关键字

        如果 Attrs 没有 static 修饰，则需要先创建 SkuEsModel 实例：
        SkuEsModel skuEsModel = new SkuEsModel();
        SkuEsModel.Attrs attrs = skuEsModel.new Attrs();  // 需要通过外部类实例创建
     */
    @Data
    public static class Attrs {
        private Long attrId;
        private String attrName;
        private String attrValue;
    }
}
