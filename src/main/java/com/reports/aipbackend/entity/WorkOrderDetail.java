package com.reports.aipbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单详情实体类
 * 用于封装工单详情页面的所有相关数据
 */
@Data
public class WorkOrderDetail {
    // 工单基本信息
    private Integer workId; // 工单ID
    private String userOpenid; // 用户openid
    private String title; // 标题
    private String description; // 描述
    private String imageUrls; // 图片URLs
    private String address; // 地址描述
    private String buildingInfo; // 楼栋信息
    private String status; // 工单状态：未领取、处理中、已上报、处理完
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
    
    // 处理信息
    private String handledBy; // 处理人
    private String handledImages; // 处理图片URLs
    private String handledDesc; // 处理描述
    private LocalDateTime feedbackTime; // 反馈时间
    
    // 处理记录列表
    private List<WorkOrderProcessing> processingLogs;
    
    // 反馈信息列表
    private List<WorkOrderFeedback> feedbackList;
    
    // 用户信息（可选，用于显示提交人信息）
    private String submitterName; // 提交人姓名
    private String submitterPhone; // 提交人电话（脱敏）
    
    // 处理人信息（可选，用于显示处理人信息）
    private String handlerName; // 处理人姓名
    private String handlerPhone; // 处理人电话（脱敏）
} 