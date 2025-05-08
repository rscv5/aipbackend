package com.reports.aipbackend.service;

import com.reports.aipbackend.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务
 * 处理文件上传、存储和URL生成
 */
@Service
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    
    @Value("${file.upload.path}")
    private String uploadPath;
    
    @Value("${file.upload.url-prefix}")
    private String urlPrefix;
    
    /**
     * 上传单个文件
     * @param file 文件
     * @param subDir 子目录
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String subDir) {
        try {
            // 1. 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;
            
            // 2. 创建目录
            Path uploadDir = Paths.get(uploadPath, subDir);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            // 3. 保存文件
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath);
            
            // 4. 返回访问URL
            return urlPrefix + "/" + subDir + "/" + filename;
        } catch (IOException e) {
            logger.error("文件上传失败", e);
            throw new BusinessException("文件上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 上传多个文件
     * @param files 文件列表
     * @param subDir 子目录
     * @return 文件访问URL列表
     */
    public List<String> uploadFiles(List<MultipartFile> files, String subDir) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                urls.add(uploadFile(file, subDir));
            }
        }
        return urls;
    }
    
    /**
     * 删除文件
     * @param url 文件URL
     */
    public void deleteFile(String url) {
        try {
            // 1. 从URL中提取文件路径
            String filePath = url.replace(urlPrefix, uploadPath);
            Path path = Paths.get(filePath);
            
            // 2. 删除文件
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            logger.error("文件删除失败", e);
            throw new BusinessException("文件删除失败：" + e.getMessage());
        }
    }
} 