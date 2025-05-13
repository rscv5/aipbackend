package com.reports.aipbackend.entity;

import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;

@Data
public class WorkOrder {
    private Integer workId; // 工单ID
    private String userOpenid; // 用户openid
    private String title; // 标题
    private String description; // 描述
    private List<String> imageUrls; // 图片URLs
    //private String location; // 位置坐标（MySQL POINT类型，格式：POINT(longitude latitude)，例如：POINT(118.77013 32.06639)）
    private String address; // 地址描述（前端传入的地址字符串）
    private String buildingInfo; // 楼栋信息
    private String status; // 工单状态：未领取、处理中、已上报、处理完
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
    private String handledBy; // 处理人
    private List<String> handledImages; // 处理图片URLs
    private String handledDesc; // 处理描述
    private LocalDateTime feedbackTime; // 反馈时间
} 