package com.reports.aipbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrderFeedback {
    private Long feedbackId; // 反馈ID
    private Integer workId; // 工单ID
    private String handlerOpenid; // 处理人openid
    private String handlerRole; // 处理人角色
    private String feedbackDescription; // 反馈描述
    private String feedbackImages; // 反馈图片URLs
    private LocalDateTime feedbackTime; // 反馈时间
} 