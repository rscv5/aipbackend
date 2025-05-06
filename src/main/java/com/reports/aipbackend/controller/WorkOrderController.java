package com.reports.aipbackend.controller;

import com.reports.aipbackend.common.Result;
import com.reports.aipbackend.entity.WorkOrder;
import com.reports.aipbackend.entity.WorkOrderFeedback;
import com.reports.aipbackend.entity.WorkOrderProcessing;
import com.reports.aipbackend.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {
    
    @Autowired
    private WorkOrderService workOrderService;
    
    // 创建工单
    @PostMapping
    public Result<WorkOrder> createWorkOrder(@RequestBody WorkOrder workOrder) {
        return Result.success(workOrderService.createWorkOrder(workOrder));
    }
    
    // 获取工单列表
    @GetMapping
    public Result<List<WorkOrder>> getWorkOrders(@RequestParam(required = false) String status) {
        return Result.success(workOrderService.getWorkOrders(status));
    }
    
    // 获取工单详情
    @GetMapping("/{workId}")
    public Result<WorkOrder> getWorkOrderDetail(@PathVariable Integer workId) {
        return Result.success(workOrderService.getWorkOrderDetail(workId));
    }
    
    // 更新工单状态
    @PutMapping("/{workId}/status")
    public Result<Void> updateWorkOrderStatus(
            @PathVariable Integer workId,
            @RequestParam String status,
            @RequestParam String operatorOpenid,
            @RequestParam String operatorRole,
            @RequestParam String actionDescription) {
        workOrderService.updateWorkOrderStatus(workId, status, operatorOpenid, operatorRole, actionDescription);
        return Result.success(null);
    }
    
    // 提交反馈
    @PostMapping("/{workId}/feedback")
    public Result<Void> submitFeedback(
            @PathVariable Integer workId,
            @RequestBody WorkOrderFeedback feedback) {
        feedback.setWorkId(workId);
        workOrderService.submitFeedback(feedback);
        return Result.success(null);
    }
    
    // 获取工单处理记录
    @GetMapping("/{workId}/processing")
    public Result<List<WorkOrderProcessing>> getWorkOrderProcessing(@PathVariable Integer workId) {
        return Result.success(workOrderService.getWorkOrderProcessing(workId));
    }
    
    // 获取工单反馈
    @GetMapping("/{workId}/feedback")
    public Result<List<WorkOrderFeedback>> getWorkOrderFeedback(@PathVariable Integer workId) {
        return Result.success(workOrderService.getWorkOrderFeedback(workId));
    }
} 