package com.reports.aipbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 文件上传配置类
 * 配置上传文件的存储路径和访问路径
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {
    
    @Value("${file.upload.path}")
    private String uploadPath;
    
    @Value("${file.upload.url-prefix}")
    private String urlPrefix;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源映射
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
} 