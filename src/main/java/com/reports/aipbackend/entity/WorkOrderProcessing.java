package com.reports.aipbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class WorkOrderProcessing {
    private Long logId; // 日志ID
    private Integer workId; // 工单ID
    private String operatorOpenid; // 操作人openid
    private String operatorRole; // 操作人角色
    private String operatorUsername; // 操作人用户名
    private String actionType; // 操作类型
    private String actionDescription; // 操作描述
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime actionTime; // 操作时间
    private String extraData; // 额外数据
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
} 