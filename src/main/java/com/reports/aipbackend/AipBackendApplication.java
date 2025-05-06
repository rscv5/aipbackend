package com.reports.aipbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.reports.aipbackend.mapper")
public class AipBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AipBackendApplication.class, args);
    }

}
