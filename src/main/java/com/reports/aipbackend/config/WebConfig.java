package com.reports.aipbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import java.io.File;
import jakarta.annotation.PostConstruct;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.url-prefix}")
    private String urlPrefix;

    public WebConfig() {
        logger.info("WebConfig constructor called - class is being instantiated");
    }

    @PostConstruct
    public void init() {
        logger.info("WebConfig initialized with uploadPath: {} and urlPrefix: {}", uploadPath, urlPrefix);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        logger.info("Starting static resource mapping configuration");
        logger.info("Upload path: {}", uploadPath);
        logger.info("URL prefix: {}", urlPrefix);
        
        // Check if upload directory exists
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            logger.error("Upload directory does not exist: {}", uploadPath);
            boolean created = uploadDir.mkdirs();
            if (created) {
                logger.info("Created upload directory: {}", uploadPath);
            } else {
                logger.error("Failed to create upload directory: {}", uploadPath);
            }
        }
        
        // Check directory permissions
        if (!uploadDir.canRead()) {
            logger.error("Upload directory is not readable: {}", uploadPath);
        }
        
        // Add resource handler
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + uploadPath + "/")
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new org.springframework.web.servlet.resource.PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws java.io.IOException {
                        Resource resource = super.getResource(resourcePath, location);
                        if (resource != null && resource.exists()) {
                            logger.info("Successfully found resource: " + resourcePath);
                            return resource;
                        } else {
                            logger.error("Resource not found: " + resourcePath);
                            return null;
                        }
                    }
                });
        
        logger.info("Static resource mapping configuration completed");
    }
}