package com.reports.aipbackend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 图片上传控制器
 * 负责接收前端上传的图片并保存到本地指定目录
 */
@RestController
@RequestMapping("/api")
public class UploadController {
    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    
    @Value("${file.upload.path}")
    private String uploadPath;
    
    @Value("${file.upload.url-prefix}")
    private String urlPrefix;

    /**
     * 图片上传接口
     * @param file 前端上传的图片文件
     * @return 图片的访问路径
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        logger.info("收到图片上传请求: 文件名={}, 大小={} bytes", file.getOriginalFilename(), file.getSize());
        
        if (file.isEmpty()) {
            logger.warn("上传失败：文件为空");
            return ResponseEntity.badRequest().body("文件为空");
        }
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            logger.warn("上传失败：不支持的文件类型 {}", contentType);
            return ResponseEntity.badRequest().body("只支持图片文件");
        }
        
        try {
            // 确保目录存在
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                logger.info("创建上传目录: {}", uploadDir);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            Path filePath = uploadDir.resolve(filename);

            // 保存文件
            Files.copy(file.getInputStream(), filePath);
            logger.info("图片保存成功: {}", filePath);

            // 返回图片的访问路径
            String imageUrl = urlPrefix + "/" + filename;
            Map<String, Object> result = new HashMap<>();
            result.put("url", imageUrl);
            result.put("filename", filename);
            logger.info("返回图片URL: {}", imageUrl);
            
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("图片上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("图片上传失败: " + e.getMessage());
        }
    }
} 