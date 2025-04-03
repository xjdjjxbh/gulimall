package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.GulimallElasticsearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class GulimallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    @Test
    void contextLoads() throws IOException {
        IndexRequest indexRequest = new IndexRequest("user");
        indexRequest.id("1");
        Person person = new Person();
        person.setName("张三");
        person.setAge(18);

        String personString = JSON.toJSONString(person);
        indexRequest.source(personString, XContentType.JSON);


        IndexResponse index = client.index(indexRequest, GulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println(index);
    }

    @Test
    void searchData() throws IOException {
        //创建索引请求
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("bank");

        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.query(QueryBuilders.matchQuery("address", "mill"));

        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age");
        AvgAggregationBuilder ageAvg = AggregationBuilders.avg("agevg").field("age");
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        builder.aggregation(ageAgg);
        builder.aggregation(balanceAvg);

//        builder.aggregation()
        searchRequest.source(builder);

        //执行索引
        SearchResponse response = client.search(searchRequest, GulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println(response.toString());
    }


    @Data
    class Person {
        String name;
        int age;
    }

}
