package com.reports.aipbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.reports.aipbackend")
@MapperScan("com.reports.aipbackend.mapper")
public class AipBackendApplication {
    private static final Logger logger = LoggerFactory.getLogger(AipBackendApplication.class);

    public static void main(String[] args) {
        logger.info("Starting AipBackend application");
        SpringApplication.run(AipBackendApplication.class, args);
        logger.info("AipBackend application started successfully");
    }
}
