package com.reports.aipbackend.service;

import com.reports.aipbackend.entity.WorkOrder;
import com.reports.aipbackend.entity.WorkOrderProcessing;
import com.reports.aipbackend.entity.WorkOrderFeedback;
import com.reports.aipbackend.mapper.WorkOrderMapper;
import com.reports.aipbackend.mapper.WorkOrderProcessingMapper;
import com.reports.aipbackend.mapper.WorkOrderFeedbackMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkOrderService {
    
    @Autowired
    private WorkOrderMapper workOrderMapper;
    
    @Autowired
    private WorkOrderProcessingMapper processingMapper;
    
    @Autowired
    private WorkOrderFeedbackMapper feedbackMapper;
    
    // 创建工单
    @Transactional
    public WorkOrder createWorkOrder(WorkOrder workOrder) {
        workOrderMapper.insert(workOrder);
        
        // 记录处理日志
        WorkOrderProcessing processing = new WorkOrderProcessing();
        processing.setWorkId(workOrder.getWorkId());
        processing.setOperatorOpenid(workOrder.getUserOpenid());
        processing.setOperatorRole("普通用户");
        processing.setActionType("提交工单");
        processing.setActionDescription("用户提交新工单");
        processingMapper.insert(processing);
        
        return workOrder;
    }
    
    // 更新工单状态
    @Transactional
    public void updateWorkOrderStatus(Integer workId, String status, String operatorOpenid, 
                                    String operatorRole, String actionDescription) {
        WorkOrder workOrder = workOrderMapper.findById(workId);
        if (workOrder != null) {
            workOrder.setStatus(status);
            workOrderMapper.update(workOrder);
            
            // 记录处理日志
            WorkOrderProcessing processing = new WorkOrderProcessing();
            processing.setWorkId(workId);
            processing.setOperatorOpenid(operatorOpenid);
            processing.setOperatorRole(operatorRole);
            processing.setActionType(status);
            processing.setActionDescription(actionDescription);
            processingMapper.insert(processing);
        }
    }
    
    // 提交反馈
    @Transactional
    public void submitFeedback(WorkOrderFeedback feedback) {
        feedbackMapper.insert(feedback);
        
        // 更新工单状态
        WorkOrder workOrder = workOrderMapper.findById(feedback.getWorkId());
        if (workOrder != null) {
            workOrder.setStatus("处理完");
            workOrder.setHandledBy(feedback.getHandlerOpenid());
            workOrder.setHandledDesc(feedback.getFeedbackDescription());
            workOrder.setHandledImages(feedback.getFeedbackImages());
            workOrder.setFeedbackTime(feedback.getFeedbackTime());
            workOrderMapper.update(workOrder);
        }
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
} 