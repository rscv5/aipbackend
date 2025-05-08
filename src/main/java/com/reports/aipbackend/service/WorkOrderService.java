package com.reports.aipbackend.service;

import com.reports.aipbackend.entity.WorkOrder;
import com.reports.aipbackend.entity.WorkOrderProcessing;
import com.reports.aipbackend.entity.WorkOrderFeedback;
import com.reports.aipbackend.mapper.WorkOrderMapper;
import com.reports.aipbackend.mapper.WorkOrderProcessingMapper;
import com.reports.aipbackend.mapper.WorkOrderFeedbackMapper;
import com.reports.aipbackend.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;

/**
 * 工单服务类
 * 处理工单相关的业务逻辑
 */
@Service
public class WorkOrderService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderService.class);
    
    @Autowired
    private WorkOrderMapper workOrderMapper;
    
    @Autowired
    private WorkOrderProcessingMapper processingMapper;
    
    @Autowired
    private WorkOrderFeedbackMapper feedbackMapper;
    
    /**
     * 创建工单
     * @param workOrder 工单信息
     * @return 创建后的工单
     */
    @Transactional
    public WorkOrder createWorkOrder(WorkOrder workOrder) {
        logger.info("开始创建工单: {}", workOrder);
        
        // 1. 验证必填字段
        if (workOrder.getUserOpenid() == null || workOrder.getUserOpenid().trim().isEmpty()) {
            logger.error("创建工单失败: 用户openid为空");
            throw new BusinessException("用户openid不能为空");
        }
        if (workOrder.getDescription() == null || workOrder.getDescription().trim().isEmpty()) {
            logger.error("创建工单失败: 描述为空");
            throw new BusinessException("描述不能为空");
        }

        if (workOrder.getAddress() == null || workOrder.getAddress().trim().isEmpty()) {
            logger.error("创建工单失败: 地址为空");
            throw new BusinessException("地址不能为空");
        }
        
        if (workOrder.getBuildingInfo() == null || workOrder.getBuildingInfo().trim().isEmpty()) {
            logger.error("创建工单失败: 楼栋信息为空");
            throw new BusinessException("楼栋信息不能为空");
        }

        // 设置默认值
        if (workOrder.getStatus() == null) {
            workOrder.setStatus("未领取");
        }
        
        LocalDateTime now = LocalDateTime.now();
        workOrder.setCreatedAt(now);
        workOrder.setUpdatedAt(now);
        
        // 3. 如果没有标题，自动生成标题（取描述的前50个字符）
        if (workOrder.getTitle() == null || workOrder.getTitle().trim().isEmpty()) {
            String description = workOrder.getDescription().trim();
            workOrder.setTitle(description.length() > 50 ? description.substring(0, 50) + "..." : description);
        }
        
        try {
            // 插入工单
            workOrderMapper.insert(workOrder);
            logger.info("工单创建成功: workId={}", workOrder.getWorkId());
            
            // 记录处理日志
            WorkOrderProcessing processing = new WorkOrderProcessing();
            processing.setWorkId(workOrder.getWorkId());
            processing.setOperatorOpenid(workOrder.getUserOpenid());
            processing.setOperatorRole("普通用户");
            processing.setActionType("提交工单");
            processing.setActionDescription("用户创建工单");
            processing.setActionTime(now);
            processingMapper.insert(processing);
            
            return workOrder;
        } catch (Exception e) {
            logger.error("工单创建失败", e);
            throw new BusinessException("工单创建失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新工单状态
     * @param workId 工单ID
     * @param status 新状态
     * @param handlerOpenid 处理人openid
     * @return 更新后的工单
     */
    @Transactional
    public WorkOrder updateWorkOrderStatus(Integer workId, String status, String handlerOpenid) {
        logger.info("开始更新工单状态: workId={}, status={}, handlerOpenid={}", workId, status, handlerOpenid);
        
        // 1. 获取工单
        WorkOrder workOrder = workOrderMapper.findById(workId);
        if (workOrder == null) {
            logger.error("更新工单状态失败: 工单不存在, workId={}", workId);
            throw new BusinessException("工单不存在");
        }
        
        // 2. 验证状态转换是否合法
        validateStatusTransition(workOrder.getStatus(), status);
        
        // 3. 更新工单
            workOrder.setStatus(status);
        workOrder.setHandledBy(handlerOpenid);
        workOrder.setUpdatedAt(LocalDateTime.now());
        
        try {
            workOrderMapper.update(workOrder);
            logger.info("工单状态更新成功: workId={}, status={}", workId, status);
            
            // 记录处理日志
            WorkOrderProcessing processing = new WorkOrderProcessing();
            processing.setWorkId(workId);
            processing.setOperatorOpenid(handlerOpenid);
            processing.setOperatorRole("处理人");
            processing.setActionType(status);
            processing.setActionDescription("处理人更新工单状态");
            processing.setActionTime(LocalDateTime.now());
            processingMapper.insert(processing);
            
            return workOrder;
        } catch (Exception e) {
            logger.error("工单状态更新失败", e);
            throw new BusinessException("工单状态更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 提交工单处理反馈
     * @param workId 工单ID
     * @param handlerOpenid 处理人openid
     * @param handledDesc 处理描述
     * @param handledImages 处理图片
     * @return 更新后的工单
     */
    @Transactional
    public WorkOrder submitWorkOrderFeedback(Integer workId, String handlerOpenid, 
            String handledDesc, String handledImages) {
        logger.info("开始提交工单反馈: workId={}, handlerOpenid={}", workId, handlerOpenid);
        
        // 1. 获取工单
        WorkOrder workOrder = workOrderMapper.findById(workId);
        if (workOrder == null) {
            logger.error("提交工单反馈失败: 工单不存在, workId={}", workId);
            throw new BusinessException("工单不存在");
        }
        
        // 2. 验证工单状态
        if (!"处理中".equals(workOrder.getStatus())) {
            logger.error("提交工单反馈失败: 工单状态不正确, status={}", workOrder.getStatus());
            throw new BusinessException("只有处理中的工单才能提交反馈");
        }
        
        // 3. 更新工单
        workOrder.setHandledDesc(handledDesc);
        workOrder.setHandledImages(handledImages);
        workOrder.setStatus("已解决");
        workOrder.setFeedbackTime(LocalDateTime.now());
        workOrder.setUpdatedAt(LocalDateTime.now());
        
        try {
            workOrderMapper.update(workOrder);
            logger.info("工单反馈提交成功: workId={}", workId);
            
            // 记录处理日志
            WorkOrderProcessing processing = new WorkOrderProcessing();
            processing.setWorkId(workId);
            processing.setOperatorOpenid(handlerOpenid);
            processing.setOperatorRole("处理人");
            processing.setActionType("提交反馈");
            processing.setActionDescription("处理人提交反馈");
            processing.setActionTime(LocalDateTime.now());
            processingMapper.insert(processing);
            
            return workOrder;
        } catch (Exception e) {
            logger.error("工单反馈提交失败", e);
            throw new BusinessException("工单反馈提交失败：" + e.getMessage());
        }
    }
    
    /**
     * 验证状态转换是否合法
     * @param oldStatus 原状态
     * @param newStatus 新状态
     */
    private void validateStatusTransition(String oldStatus, String newStatus) {
        // 定义合法的状态转换
        Map<String, List<String>> validTransitions = new HashMap<>();
        validTransitions.put("未领取", Arrays.asList("处理中", "已上报"));
        validTransitions.put("处理中", Arrays.asList("已上报", "处理完"));
        validTransitions.put("已上报", Arrays.asList("处理中", "处理完"));
        validTransitions.put("处理完", Collections.emptyList());

        if (!validTransitions.containsKey(oldStatus)) {
            throw new BusinessException("无效的原状态: " + oldStatus);
        }
        if (!validTransitions.get(oldStatus).contains(newStatus)) {
            throw new BusinessException("不允许从 " + oldStatus + " 转换到 " + newStatus);
        }
    }
    
    /**
     * 获取用户的工单列表
     * @param userOpenid 用户openid
     * @return 工单列表
     */
    public List<WorkOrder> getUserWorkOrders(String userOpenid) {
        logger.info("获取用户工单列表: userOpenid={}", userOpenid);
        return workOrderMapper.findByUserOpenid(userOpenid);
    }
    
    /**
     * 获取处理人的工单列表
     * @param handlerOpenid 处理人openid
     * @return 工单列表
     */
    public List<WorkOrder> getHandlerWorkOrders(String handlerOpenid) {
        logger.info("获取处理人工单列表: handlerOpenid={}", handlerOpenid);
        return workOrderMapper.findByHandlerOpenid(handlerOpenid);
    }
    
    /**
     * 获取指定状态的工单列表
     * @param status 状态
     * @return 工单列表
     */
    public List<WorkOrder> getWorkOrdersByStatus(String status) {
        logger.info("获取指定状态的工单列表: status={}", status);
        return workOrderMapper.findByStatus(status);
    }
    
    // 获取工单列表
    public List<WorkOrder> getWorkOrders(String status) {
        if (status != null && !status.isEmpty()) {
            return workOrderMapper.findByStatus(status);
        }
        return workOrderMapper.findAll();
    }
    
    // 获取工单详情
    public WorkOrder getWorkOrderDetail(Integer workId) {
        return workOrderMapper.findById(workId);
    }
    
    // 获取工单处理记录
    public List<WorkOrderProcessing> getWorkOrderProcessing(Integer workId) {
        return processingMapper.findByWorkId(workId);
    }
    
    // 获取工单反馈
    public List<WorkOrderFeedback> getWorkOrderFeedback(Integer workId) {
        return feedbackMapper.findByWorkId(workId);
    }

    /**
     * 根据ID查询工单
     * @param workId 工单ID
     * @return 工单信息
     */
    public WorkOrder findById(Integer workId) {
        logger.info("查询工单: workId={}", workId);
        WorkOrder workOrder = workOrderMapper.findById(workId);
        if (workOrder == null) {
            throw new BusinessException("工单不存在");
        }
        return workOrder;
    }

    /**
     * 根据用户openid查询工单列表
     * @param userOpenid 用户openid
     * @return 工单列表
     */
    public List<WorkOrder> findByUserOpenid(String userOpenid) {
        logger.info("查询用户工单列表: userOpenid={}", userOpenid);
        return workOrderMapper.findByUserOpenid(userOpenid);
    }

    /**
     * 更新工单状态
     * @param workId 工单ID
     * @param status 新状态
     * @param handledBy 处理人
     * @param handledDesc 处理描述
     * @param handledImages 处理图片
     * @return 更新后的工单
     */
    @Transactional
    public WorkOrder updateStatus(Integer workId, String status, String handledBy, 
            String handledDesc, String handledImages) {
        logger.info("更新工单状态: workId={}, status={}, handledBy={}", workId, status, handledBy);
        
        WorkOrder workOrder = findById(workId);
        validateStatusTransition(workOrder.getStatus(), status);
        
        workOrder.setStatus(status);
        workOrder.setHandledBy(handledBy);
        workOrder.setHandledDesc(handledDesc);
        workOrder.setHandledImages(handledImages);
        workOrder.setUpdatedAt(LocalDateTime.now());
        
        if ("处理完".equals(status)) {
            workOrder.setFeedbackTime(LocalDateTime.now());
        }
        
        workOrderMapper.update(workOrder);
        return workOrder;
    }

    /**
     * 根据状态查询工单列表
     * @param status 状态
     * @return 工单列表
     */
    public List<WorkOrder> findByStatus(String status) {
        logger.info("查询状态工单列表: status={}", status);
        return workOrderMapper.findByStatus(status);
    }

    /**
     * 查询所有工单
     * @return 工单列表
     */
    public List<WorkOrder> findAll() {
        logger.info("查询所有工单");
        return workOrderMapper.findAll();
    }
} 