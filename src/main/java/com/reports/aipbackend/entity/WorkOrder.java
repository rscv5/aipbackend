package com.reports.aipbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrder {
    private Integer workId; // 工单ID
    private String userOpenid; // 用户openid
    private String title; // 标题
    private String description; // 描述
    private String imageUrls; // 图片URLs
    private String location; // 位置
    private String buildingInfo; // 楼栋信息
    private String status; // 工单状态
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
    private String handledBy; // 处理人
    private String handledImages; // 处理图片URLs
    private String handledDesc; // 处理描述
    private LocalDateTime feedbackTime; // 反馈时间
} 