package com.reports.aipbackend.controller;

import com.reports.aipbackend.common.Result;
import com.reports.aipbackend.entity.WorkOrder;
import com.reports.aipbackend.entity.WorkOrderFeedback;
import com.reports.aipbackend.entity.WorkOrderProcessing;
import com.reports.aipbackend.entity.WorkOrderDetail;
import com.reports.aipbackend.service.WorkOrderService;
import com.reports.aipbackend.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
     * @param params 工单信息和手机号
     * @return 创建结果
     */
    @PostMapping("/create")
    public Result<WorkOrder> createWorkOrder(@RequestBody Map<String, Object> params) {
        logger.info("开始创建工单，接收到的参数: {}", params);
        try {
            // 解析工单参数
            WorkOrder workOrder = new WorkOrder();
            
            // 必填字段验证
            String userOpenid = (String) params.get("userOpenid");
            if (userOpenid == null || userOpenid.trim().isEmpty()) {
                logger.error("创建工单失败: userOpenid为空");
                return Result.error("用户标识不能为空");
            }
            workOrder.setUserOpenid(userOpenid);
            
            String description = (String) params.get("description");
            if (description == null || description.trim().isEmpty()) {
                logger.error("创建工单失败: description为空");
                return Result.error("问题描述不能为空");
            }
            workOrder.setDescription(description);
            
            // 处理图片URL列表
            Object imageUrlsObj = params.get("imageUrls");
            if (imageUrlsObj != null) {
                try {
                    @SuppressWarnings("unchecked")
                    List<String> imageUrls = (List<String>) imageUrlsObj;
                    logger.info("接收到的图片URL列表: {}", imageUrls);
                    workOrder.setImageUrls(imageUrls);
                } catch (ClassCastException e) {
                    logger.error("图片URL列表格式错误: {}", e.getMessage());
                    return Result.error("图片URL列表格式错误");
                }
            } else {
                workOrder.setImageUrls(new ArrayList<>());
            }
            
            // 处理地址信息
            String address = (String) params.get("address");
            if (address == null || address.trim().isEmpty()) {
                logger.error("创建工单失败: address为空");
                return Result.error("地址信息不能为空");
            }
            workOrder.setAddress(address);
            
            // 处理楼栋信息
            String buildingInfo = (String) params.get("buildingInfo");
            if (buildingInfo == null || buildingInfo.trim().isEmpty()) {
                logger.error("创建工单失败: buildingInfo为空");
                return Result.error("楼栋信息不能为空");
            }
            workOrder.setBuildingInfo(buildingInfo);
            
            // 设置工单状态
            String status = (String) params.get("status");
            workOrder.setStatus(status != null ? status : "未领取");
            
            // 解析手机号
            String phone = (String) params.get("phone");
            if (phone == null || phone.trim().isEmpty()) {
                logger.error("创建工单失败: phone为空");
                return Result.error("手机号不能为空");
            }
            
            logger.info("工单参数解析完成，准备创建工单: {}", workOrder);
            WorkOrder createdOrder = workOrderService.createWorkOrder(workOrder, phone);
            logger.info("工单创建成功: workId={}", createdOrder.getWorkId());
            
            return Result.success(createdOrder);
        } catch (BusinessException e) {
            logger.error("创建工单业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("创建工单系统异常", e);
            return Result.error("系统错误，请稍后重试");
        }
    }
    
    /**
     * 获取工单详情
     * @param workId 工单ID
     * @return 工单详情（包含处理记录和反馈信息）
     */
    @GetMapping("/{workId}")
    public Result<WorkOrderDetail> getWorkOrder(@PathVariable Integer workId) {
        logger.info("获取工单详情请求: workId={}", workId);
        try {
            WorkOrderDetail detail = workOrderService.getWorkOrderDetail(workId);
            logger.info("获取工单详情成功: workId={}", workId);
            return Result.success(detail);
        } catch (BusinessException e) {
            logger.error("获取工单详情失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("获取工单详情失败", e);
            return Result.error("系统错误，请稍后重试");
        }
    }
    
    /**
     * 获取用户的工单列表
     * @param userOpenid 用户openid
     * @param status 工单状态（可选）
     * @return 工单列表
     */
    @GetMapping("/user")
    public Result<List<WorkOrder>> getUserWorkOrders(
            @RequestParam String userOpenid,
            @RequestParam(required = false) String status) {
        logger.info("获取用户工单列表请求: userOpenid={}, status={}", userOpenid, status);
        try {
            List<WorkOrder> workOrders = workOrderService.getUserWorkOrdersByStatus(userOpenid, status);
            logger.info("获取用户工单列表成功: 共{}条记录", workOrders.size());
            return Result.success(workOrders);
        } catch (Exception e) {
            logger.error("获取用户工单列表失败", e);
            return Result.error("获取工单列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新工单状态
     * @param workId 工单ID
     * @param status 新状态
     * @param handledBy 处理人
     * @param handledDesc 处理描述
     * @param handledImages 处理图片URL列表
     * @return 更新结果
     */
    @PreAuthorize("hasAnyRole('GRID', 'ADMIN')")
    @PutMapping("/{workId}/status")
    public Result<WorkOrder> updateWorkOrderStatus(
            @PathVariable Integer workId,
            @RequestParam String status,
            @RequestParam(required = false) String handledBy,
            @RequestParam(required = false) String handledDesc,
            @RequestParam(required = false) List<String> handledImages) {
        logger.info("更新工单状态请求: workId={}, status={}, handledBy={}, handledImages={}", 
            workId, status, handledBy, handledImages);
        try {
            WorkOrder updatedOrder = workOrderService.updateStatus(
                workId, status, handledBy, handledDesc, 
                handledImages != null ? handledImages : new ArrayList<>()
            );
            logger.info("工单状态更新成功: workId={}, status={}", workId, status);
            return Result.success(updatedOrder);
        } catch (BusinessException e) {
            logger.error("更新工单状态失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("更新工单状态失败", e);
            return Result.error("系统错误，请稍后重试");
        }
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