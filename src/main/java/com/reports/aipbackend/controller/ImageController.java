package com.reports.aipbackend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/img_test")
@CrossOrigin(originPatterns = {"http://localhost:[*]", "http://127.0.0.1:[*]", "https://servicewechat.com", "https://*.weixin.qq.com"}, 
             allowedHeaders = {"*"},
             exposedHeaders = {"*"},
             methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
             allowCredentials = "false")
public class ImageController {
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Value("${file.upload.path}")
    private String uploadPath;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        logger.info("Image request received: {}", filename);
        
        try {
            File file = new File(uploadPath, filename);
            logger.info("Full image path: {}", file.getAbsolutePath());
            
            if (!file.exists()) {
                logger.error("Image not found: {}", file.getAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            if (!file.canRead()) {
                logger.error("Image cannot be read: {}", file.getAbsolutePath());
                return ResponseEntity.status(403).build();
            }
            
            Resource resource = new FileSystemResource(file);
            logger.info("Successfully loaded image: {}", filename);
            
            // Determine content type based on file extension
            String contentType = "image/jpeg";
            if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("Error processing image request", e);
            return ResponseEntity.status(500).build();
        }
    }
} 