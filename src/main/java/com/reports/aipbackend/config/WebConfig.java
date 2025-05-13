package com.reports.aipbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源映射配置
 * 将/img_test/** 映射到本地图片目录，供前端访问上传的图片
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射/img_test/** 到本地目录C:/Users/15535/Desktop/img_test/
        registry.addResourceHandler("/img_test/**")
                .addResourceLocations("file:/C:/Users/15535/Desktop/img_test/");
    }
} 