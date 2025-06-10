package com.reports.aipbackend.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class COSConfig {

    @Value("${COS_REGION}")
    private String region;

    @Value("${COS_BUCKET_NAME}")
    private String bucketName;

    @Value("${cos.domain}")
    private String domain;

    @Bean
    public COSClient cosClient() {
        // 1 初始化用户身份信息(依赖云开发环境自动获取凭证)
        // COSCredentials cred = new BasicCOSCredentials(secretId, secretKey); // 不再需要显式凭证
        // 2 设置 bucket 的区域, COS 地域的简称请参照 https://cloud.tencent.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 3 生成 cos 客户端
        return new COSClient(clientConfig); // 使用不带凭证的构造函数
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getRegion() {
        return region;
    }

    public String getDomain() {
        return domain;
    }
} 