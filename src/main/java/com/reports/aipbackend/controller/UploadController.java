package com.reports.aipbackend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;
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
    // 本地图片保存目录
    private static final String IMG_DIR = "C:\\Users\\15535\\Desktop\\img_test";

    /**
     * 图片上传接口
     * @param file 前端上传的图片文件
     * @return 图片的本地访问路径
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        logger.info("收到图片上传请求: 文件名={}", file.getOriginalFilename());
        if (file.isEmpty()) {
            logger.warn("上传失败：文件为空");
            return ResponseEntity.badRequest().body("文件为空");
        }
        try {
            // 确保目录存在
            File dir = new File(IMG_DIR);
            if (!dir.exists()) dir.mkdirs();

            // 生成唯一文件名，防止重名覆盖
            String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'));
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            File dest = new File(dir, filename);

            // 保存文件
            file.transferTo(dest);
            logger.info("图片保存成功: {}", dest.getAbsolutePath());

            // 返回图片的本地访问路径（供前端存储和展示）
            Map<String, Object> result = new HashMap<>();
            result.put("url", "/img_test/" + filename);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("图片上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("图片上传失败");
        }
    }
} 