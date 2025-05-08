package com.reports.aipbackend.controller;

import com.reports.aipbackend.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 * 处理文件上传和删除请求
 */
@RestController
@RequestMapping("/api/files")
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    
    @Autowired
    private FileService fileService;
    
    /**
     * 上传单个文件
     * @param file 文件
     * @param type 文件类型（用于确定存储子目录）
     * @return 文件URL
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {
        try {
            logger.info("开始上传文件: {}, 类型: {}", file.getOriginalFilename(), type);
            String url = fileService.uploadFile(file, type);
            logger.info("文件上传成功: {}", url);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", url);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("文件上传失败", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * 上传多个文件
     * @param files 文件列表
     * @param type 文件类型
     * @return 文件URL列表
     */
    @PostMapping("/upload/multiple")
    public ResponseEntity<?> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("type") String type) {
        try {
            logger.info("开始批量上传文件, 类型: {}, 文件数: {}", type, files.size());
            List<String> urls = fileService.uploadFiles(files, type);
            logger.info("批量文件上传成功, 上传数量: {}", urls.size());
            
            Map<String, List<String>> response = new HashMap<>();
            response.put("urls", urls);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("批量文件上传失败", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * 删除文件
     * @param url 文件URL
     * @return 操作结果
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestParam("url") String url) {
        try {
            logger.info("开始删除文件: {}", url);
            fileService.deleteFile(url);
            logger.info("文件删除成功");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("文件删除失败", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
} 