package com.reports.aipbackend.controller;

import com.reports.aipbackend.common.Result;
import com.reports.aipbackend.entity.WorkOrder;
import com.reports.aipbackend.entity.WorkOrderFeedback;
import com.reports.aipbackend.entity.WorkOrderProcessing;
import com.reports.aipbackend.service.WorkOrderService;
import com.reports.aipbackend.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单控制器
 * 处理工单相关的HTTP请求
 */
@RestController
@RequestMapping("/api/workorder")
public class WorkOrderController {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderController.class);
    
    @Autowired
    private WorkOrderService workOrderService;
    
    /**
     * 创建工单
     * @param workOrder 工单信息
     * @return 创建结果
     */
    @PostMapping("/create")
    public Result<WorkOrder> createWorkOrder(@RequestBody WorkOrder workOrder) {
        return Result.success(workOrderService.createWorkOrder(workOrder));
    }
    
    /**
     * 获取工单详情
     * @param workId 工单ID
     * @return 工单详情
     */
    @GetMapping("/{workId}")
    public Result<WorkOrder> getWorkOrder(@PathVariable Integer workId) {
        return Result.success(workOrderService.findById(workId));
    }
    
    /**
     * 获取用户的工单列表
     * @param userOpenid 用户openid
     * @return 工单列表
     */
    @GetMapping("/user/{userOpenid}")
    public Result<List<WorkOrder>> getUserWorkOrders(@PathVariable String userOpenid) {
        return Result.success(workOrderService.findByUserOpenid(userOpenid));
    }
    
    /**
     * 更新工单状态
     * @param workId 工单ID
     * @param status 新状态
     * @param handledBy 处理人
     * @param handledDesc 处理描述
     * @param handledImages 处理图片
     * @return 更新结果
     */
    @PreAuthorize("hasAnyRole('GRID', 'ADMIN')")
    @PutMapping("/{workId}/status")
    public Result<WorkOrder> updateWorkOrderStatus(
            @PathVariable Integer workId,
            @RequestParam String status,
            @RequestParam(required = false) String handledBy,
            @RequestParam(required = false) String handledDesc,
            @RequestParam(required = false) String handledImages) {
        return Result.success(workOrderService.updateStatus(workId, status, handledBy, handledDesc, handledImages));
    }
    
    /**
     * 获取指定状态的工单列表
     * @param status 状态
     * @return 工单列表
     */
    @PreAuthorize("hasAnyRole('GRID', 'ADMIN')")
    @GetMapping("/status/{status}")
    public Result<List<WorkOrder>> getWorkOrdersByStatus(@PathVariable String status) {
        return Result.success(workOrderService.findByStatus(status));
    }
    
    /**
     * 获取所有工单
     * @return 工单列表
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public Result<List<WorkOrder>> getAllWorkOrders() {
        return Result.success(workOrderService.findAll());
    }
    
    /**
     * 获取处理人的工单列表
     * @param handlerOpenid 处理人openid
     * @return 工单列表
     */
    @GetMapping("/handler/{handlerOpenid}")
    public ResponseEntity<?> getHandlerWorkOrders(@PathVariable String handlerOpenid) {
        try {
            logger.info("收到获取处理人工单列表请求: handlerOpenid={}", handlerOpenid);
            List<WorkOrder> workOrders = workOrderService.getHandlerWorkOrders(handlerOpenid);
            logger.info("获取处理人工单列表成功: handlerOpenid={}, count={}", 
                    handlerOpenid, workOrders.size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("workOrders", workOrders);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取处理人工单列表失败", e);
            return ResponseEntity.badRequest().body("系统错误，请稍后重试");
        }
    }
    
    // 获取工单列表
    @GetMapping
    public Result<List<WorkOrder>> getWorkOrders(@RequestParam(required = false) String status) {
        return Result.success(workOrderService.getWorkOrders(status));
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