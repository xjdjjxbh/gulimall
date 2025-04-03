package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.OSS;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Autowired
    private OSS ossClient;

    @Test
    void test() throws Exception {
        InputStream inputStream = Files.newInputStream(Paths.get("E:\\plblCode\\deep-learning-for-image-processing-master\\deep-learning-for-image-processing-master\\data_set\\flower_data\\train\\daisy\\5547758_eea9edfd54_n.jpg"));

        ossClient.putObject("gulimall-liuchong", "测试上传1.jpg", inputStream);
        ossClient.shutdown();
        System.out.println("上传成功");
    }
}
