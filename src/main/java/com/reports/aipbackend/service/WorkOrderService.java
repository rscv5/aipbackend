package com.reports.aipbackend.service;

import com.reports.aipbackend.entity.WorkOrder;
import com.reports.aipbackend.entity.WorkOrderProcessing;
import com.reports.aipbackend.entity.WorkOrderFeedback;
import com.reports.aipbackend.entity.WorkOrderDetail;
import com.reports.aipbackend.entity.User;
import com.reports.aipbackend.mapper.WorkOrderMapper;
import com.reports.aipbackend.mapper.WorkOrderProcessingMapper;
import com.reports.aipbackend.mapper.WorkOrderFeedbackMapper;
import com.reports.aipbackend.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
    
    @Autowired
    private UserService userService;
    
    /**
     * 创建工单（支持同步手机号）
     * @param workOrder 工单信息
     * @param phone 手机号
     * @return 创建的工单
     */
    @Transactional
    public WorkOrder createWorkOrder(WorkOrder workOrder, String phone) {
        logger.info("开始创建工单: {}，手机号: {}", workOrder, phone);
        // 1. 检查用户是否存在
        User user = userService.getUserByOpenid(workOrder.getUserOpenid());
        if (user == null) {
            logger.error("创建工单失败: 用户不存在, openid={}", workOrder.getUserOpenid());
            throw new BusinessException("用户不存在");
        }
        // 2. 检查是否重复提交
        // 获取用户最近1分钟内提交的工单
        List<WorkOrder> recentOrders = workOrderMapper.findRecentOrdersByUser(
            workOrder.getUserOpenid(),
            LocalDateTime.now().minusMinutes(1)
        );
        
        // 检查是否存在内容相似的工单
        for (WorkOrder recentOrder : recentOrders) {
            if (isSimilarOrder(recentOrder, workOrder)) {
                logger.warn("检测到重复提交工单: userOpenid={}, recentOrderId={}", 
                    workOrder.getUserOpenid(), recentOrder.getWorkId());
                throw new BusinessException("请勿重复提交相同内容的工单，请等待1分钟后再试");
            }
        }
        try {
            // 3. 设置工单初始状态
            workOrder.setStatus("未领取");
            workOrder.setCreatedAt(LocalDateTime.now());
            workOrder.setUpdatedAt(LocalDateTime.now());
            
            // 4. 保存工单
            workOrderMapper.insert(workOrder);
            // 新增：同步手机号到 user 表
            if (phone != null && workOrder.getUserOpenid() != null) {
                if (user.getPhoneNumber() == null || !user.getPhoneNumber().equals(phone)) {
                    user.setPhoneNumber(phone);
                    userService.updateUser(user);
                }
            }
            logger.info("工单创建成功: workId={}", workOrder.getWorkId());
            WorkOrderProcessing processing = new WorkOrderProcessing();
            processing.setWorkId(workOrder.getWorkId());
            processing.setOperatorOpenid(workOrder.getUserOpenid());
            processing.setOperatorRole("普通用户");
            processing.setActionType("提交工单");
            processing.setActionTime(LocalDateTime.now());
            processing.setActionDescription("用户提交工单");
            processingMapper.insert(processing);
            return workOrder;
        } catch (Exception e) {
            logger.error("工单创建失败", e);
            throw new BusinessException("工单创建失败：" + e.getMessage());
        }
    }
    
    /**
     * 判断两个工单是否相似
     * @param order1 工单1
     * @param order2 工单2
     * @return 是否相似
     */
    private boolean isSimilarOrder(WorkOrder order1, WorkOrder order2) {
        // 1. 检查描述内容相似度
        String desc1 = order1.getDescription();
        String desc2 = order2.getDescription();
        
        // 如果描述完全相同，直接返回true
        if (desc1.equals(desc2)) {
            return true;
        }
        
        // 计算描述内容的相似度（使用编辑距离算法）
        double similarity = calculateTextSimilarity(desc1, desc2);
        
        // 如果相似度超过90%，认为是相似工单
        return similarity >= 0.9;
    }
    
    /**
     * 计算两个文本的相似度
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度（0-1之间）
     */
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        // 使用编辑距离算法计算相似度
        int distance = calculateLevenshteinDistance(text1, text2);
        int maxLength = Math.max(text1.length(), text2.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * 计算两个字符串的编辑距离
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 编辑距离
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1])) + 1;
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
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

    /**
     * 获取用户的工单列表（支持状态筛选）
     * @param userOpenid 用户openid
     * @param status 工单状态（可选，null表示全部）
     * @return 工单列表
     */
    public List<WorkOrder> getUserWorkOrdersByStatus(String userOpenid, String status) {
        logger.info("获取用户工单列表: userOpenid={}, status={}", userOpenid, status);
        if (userOpenid == null || userOpenid.trim().isEmpty()) {
            logger.error("获取用户工单列表失败: userOpenid为空");
            throw new BusinessException("用户openid不能为空");
        }
        
        // 处理多状态查询
        List<String> statusList = null;
        if (status != null && !status.equals("全部")) {
            statusList = Arrays.asList(status.split(","));
        }
        
        return workOrderMapper.findByUserOpenidAndStatus(userOpenid, status, statusList);
    }

    /**
     * 获取工单详情（包含处理记录和反馈信息）
     * @param workId 工单ID
     * @return 工单详情
     */
    public WorkOrderDetail getWorkOrderDetail(Integer workId) {
        logger.info("获取工单详情: workId={}", workId);
        
        // 1. 获取工单基本信息
        WorkOrder workOrder = findById(workId);
        if (workOrder == null) {
            logger.error("获取工单详情失败: 工单不存在, workId={}", workId);
            throw new BusinessException("工单不存在");
        }
        
        // 2. 获取处理记录
        List<WorkOrderProcessing> processingLogs = processingMapper.findByWorkId(workId);
        
        // 3. 获取反馈信息
        List<WorkOrderFeedback> feedbackList = feedbackMapper.findByWorkId(workId);
        
        // 4. 获取用户信息
        User submitter = userService.getUserByOpenid(workOrder.getUserOpenid());
        
        User handler = workOrder.getHandledBy() != null ? 
            userService.getUserByOpenid(workOrder.getHandledBy()) : null;
        
        logger.info("submitter: {}", submitter);
        
        
        // 5. 组装工单详情数据
        WorkOrderDetail detail = new WorkOrderDetail();
        // 复制工单基本信息
        detail.setWorkId(workOrder.getWorkId());
        detail.setUserOpenid(workOrder.getUserOpenid());
        //detail.setTitle(workOrder.getTitle());
        
        detail.setDescription(workOrder.getDescription());
        detail.setImageUrls(workOrder.getImageUrls());
        detail.setAddress(workOrder.getAddress());
        detail.setBuildingInfo(workOrder.getBuildingInfo());
        detail.setStatus(workOrder.getStatus());
        detail.setCreatedAt(workOrder.getCreatedAt());
        detail.setUpdatedAt(workOrder.getUpdatedAt());
        detail.setHandledBy(workOrder.getHandledBy());
        detail.setHandledImages(workOrder.getHandledImages());
        detail.setHandledDesc(workOrder.getHandledDesc());
        detail.setFeedbackTime(workOrder.getFeedbackTime());
        
        // 设置处理记录和反馈信息
        detail.setProcessingLogs(processingLogs);
        detail.setFeedbackList(feedbackList);
        
        // 设置用户信息（如果存在）
        if (submitter != null) {
            detail.setSubmitterName(submitter.getUsername());
            
            String phone = submitter.getPhoneNumber();
            if (phone != null && phone.length() == 11) {
                // 手机号脱敏处理
                //detail.setSubmitterPhone(phone.substring(0, 3) + "****" + phone.substring(7));
                detail.setSubmitterPhone(phone);
                logger.info("submitter phone: {}", phone);
            }
        }
        
        // 设置处理人信息（如果存在）
        if (handler != null) {
            detail.setHandlerName(handler.getUsername());
            // 手机号脱敏处理
            String phone = handler.getPhoneNumber();
            if (phone != null && phone.length() == 11) {
                detail.setHandlerPhone(phone.substring(0, 3) + "****" + phone.substring(7));
            }
        }
        
        logger.info("工单详情获取成功: workId={}", workId);
        return detail;
    }
} 