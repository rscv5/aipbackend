package com.reports.aipbackend.controller;

import com.reports.aipbackend.config.COSConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 图片上传控制器
 * 负责接收前端上传的图片并保存到腾讯云COS
 */
@RestController
@RequestMapping("/api")
public class UploadController {
    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    
    @Autowired
    private COSClient cosClient;

    @Autowired
    private COSConfig cosConfig;

    /**
     * 图片上传接口
     * @param file 前端上传的图片文件
     * @param type 图片类型，可选
     * @param form 表单数据
     * @return 图片的访问路径
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam Map<String, String> form) {
        // 兼容：优先用单独type参数，没有则用form.get("type")
        if ((type == null || type.isEmpty()) && form != null) {
            type = form.get("type");
        }
        logger.info("收到图片上传请求: 文件名={}, 大小={} bytes, type={}, form={}", 
            file.getOriginalFilename(), file.getSize(), type, form);
        
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
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            
            // 构造 COS 文件路径 (Key)
            String cosKey = (type != null && !type.isEmpty() ? (type + "/") : "") + filename;
            logger.info("准备上传到COS: bucketName={}, key={}", cosConfig.getBucketName(), cosKey);

            // 设置文件元数据
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(contentType);

            // 构造上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                cosConfig.getBucketName(), cosKey, file.getInputStream(), objectMetadata
            );

            // 执行上传
            cosClient.putObject(putObjectRequest);
            logger.info("图片已成功上传到COS: {}", cosKey);

            // 返回图片的访问URL
            String imageUrl = String.format("%s/%s", cosConfig.getDomain(), cosKey);
            Map<String, Object> result = new HashMap<>();
            result.put("url", imageUrl);
            result.put("filename", filename); // 可选，保留原字段
            logger.info("返回图片URL: {}", imageUrl);
            
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("图片上传到COS失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("图片上传失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("图片上传发生未知错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("图片上传失败: " + e.getMessage());
        }
    }
} 