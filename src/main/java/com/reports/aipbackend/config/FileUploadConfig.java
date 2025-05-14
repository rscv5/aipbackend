package com.reports.aipbackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * 文件上传配置类
 * 配置上传文件的存储路径和访问路径
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(FileUploadConfig.class);
    
    @Value("${file.upload.path}")
    private String uploadPath;
    
    @Value("${file.upload.url-prefix}")
    private String urlPrefix;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        logger.info("配置静态资源映射: {} -> {}", urlPrefix, uploadPath);
        
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + uploadPath + "/")
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = super.getResource(resourcePath, location);
                        if (resource != null && resource.exists()) {
                            String resourceUrl = resource.getURL().toString();
                            if (logger.isDebugEnabled()) {
                                logger.debug("成功找到资源: " + resourceUrl);
                            }
                            return resource;
                        } else {
                            if (logger.isWarnEnabled()) {
                                logger.warn("未找到资源: " + resourcePath);
                            }
                            return null;
                        }
                    }
                });
        
        logger.info("静态资源映射配置完成");
    }
} 