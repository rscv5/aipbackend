package com.reports.aipbackend.controller;

import com.reports.aipbackend.service.CloudBaseStorageService;
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
    private CloudBaseStorageService cloudBaseStorageService;

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
            String filePathPrefix = (type != null && !type.isEmpty() ? (type + "/") : "workorder/"); // 默认路径

            // 1. 获取文件上传链接和COS凭证
            Map<String, Object> uploadLinkInfo = cloudBaseStorageService.getUploadFileLink(filename, filePathPrefix);
            if (uploadLinkInfo == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("获取文件上传链接失败");
            }

            // 2. 执行文件上传到COS
            boolean uploadSuccess = cloudBaseStorageService.uploadFileToCos(uploadLinkInfo, file.getBytes(), filename, filePathPrefix);
            if (!uploadSuccess) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("文件上传到COS失败");
            }

            // 上传成功后，获取文件的cos_file_id，并用它来获取下载链接
            String fileId = (String) uploadLinkInfo.get("file_id");
            if (fileId == null) {
                logger.error("上传成功但未获取到file_id");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("文件上传成功，但无法获取文件ID");
            }

            String imageUrl = cloudBaseStorageService.getDownloadUrlByFileId(fileId);
            if (imageUrl == null) {
                logger.error("获取文件下载链接失败，fileId: {}", fileId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("文件上传成功，但获取下载链接失败");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("url", imageUrl);
            result.put("filename", filename);
            logger.info("图片上传成功，返回URL: {}", imageUrl);
            
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("处理文件输入流或上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("图片上传失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("图片上传发生未知错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("图片上传失败: " + e.getMessage());
        }
    }
} 