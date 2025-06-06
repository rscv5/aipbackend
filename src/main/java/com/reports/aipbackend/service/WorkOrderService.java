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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.stream.Collectors;

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
    
    @Value("${server.address:localhost}")
    private String serverAddress;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    /**
     * 创建工单（支持同步手机号）
     * @param workOrder 工单信息
     * @param phone 手机号
     * @return 创建的工单
     */
    @Transactional
    public WorkOrder createWorkOrder(WorkOrder workOrder, String phone) {
        logger.info("开始创建工单: {}，手机号: {}", workOrder, phone);
        logger.info("收到工单创建请求");
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
     * @param handledImages 处理图片URL列表
     * @return 更新后的工单
     */
    @Transactional
    public WorkOrder updateStatus(Integer workId, String status, String handledBy, 
            String handledDesc, List<String> handledImages) {
        logger.info("更新工单状态: workId={}, status={}, handledBy={}, handledImages={}", 
            workId, status, handledBy, handledImages);
        
        WorkOrder workOrder = findById(workId);
        validateStatusTransition(workOrder.getStatus(), status);
        
        workOrder.setStatus(status);
        workOrder.setHandledBy(handledBy);
        workOrder.setHandledDesc(handledDesc);
        workOrder.setHandledImages(handledImages != null ? handledImages : new ArrayList<>());
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
        
        // 统一赋值：将 actionTime 赋值给 createdAt 和 updatedAt
        if (processingLogs != null) {
            for (WorkOrderProcessing log : processingLogs) {
                log.setCreatedAt(log.getActionTime());
                log.setUpdatedAt(log.getActionTime());
            }
        }
            
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
        detail.setDescription(workOrder.getDescription());
        
        // 处理图片URL，添加服务器地址
        String baseUrl = "http://" + serverAddress + ":" + serverPort;
        if (workOrder.getImageUrls() != null) {
            List<String> fullImageUrls = workOrder.getImageUrls().stream()
                .map(url -> {
                    if (url.startsWith("http")) {
                        return url;
                    }
                    // 确保URL以/开头
                    String path = url.startsWith("/") ? url : "/" + url;
                    return baseUrl + path;
                })
                .collect(Collectors.toList());
            detail.setImageUrls(fullImageUrls);
        }
        
        if (workOrder.getHandledImages() != null) {
            List<String> fullHandledImageUrls = workOrder.getHandledImages().stream()
                .map(url -> {
                    if (url.startsWith("http")) {
                        return url;
                    }
                    // 确保URL以/开头
                    String path = url.startsWith("/") ? url : "/" + url;
                    return baseUrl + path;
                })
                .collect(Collectors.toList());
            detail.setHandledImages(fullHandledImageUrls);
        }
        
        detail.setAddress(workOrder.getAddress());
        detail.setBuildingInfo(workOrder.getBuildingInfo());
        detail.setStatus(workOrder.getStatus());
        detail.setCreatedAt(workOrder.getCreatedAt());
        detail.setUpdatedAt(workOrder.getUpdatedAt());
        detail.setHandledBy(workOrder.getHandledBy());
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
                detail.setSubmitterPhone(phone);
                logger.info("submitter phone: {}", phone);
            }
        }
        
        // 设置处理人信息（如果存在）
        if (handler != null) {
            detail.setHandlerName(handler.getUsername());
            String phone = handler.getPhoneNumber();
            if (phone != null && phone.length() == 11) {
                detail.setHandlerPhone(phone.substring(0, 3) + "****" + phone.substring(7));
            }
        }
        
        logger.info("工单详情获取成功: workId={}", workId);
        return detail;
    }

    /**
     * 获取过去24小时未领取的工单列表
     * @return 工单列表
     */
    public List<WorkOrder> getTodayUnclaimedOrders() {
        logger.info("获取过去24小时未领取工单列表");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusHours(24);
        List<WorkOrder> orders = workOrderMapper.findByStatusAndCreatedAtBetween("未领取", startTime, now);
        // 为每个工单添加用户信息
        for (WorkOrder order : orders) {
            User user = userService.getUserByOpenid(order.getUserOpenid());
            if (user != null) {
                order.setPhoneNumber(user.getPhoneNumber());
            }
        }
        return orders;
    }

    /**
     * 获取处理人的工单列表
     * @param handlerOpenid 处理人openid
     * @return 工单列表
     */
    public List<WorkOrder> getHandlerWorkOrders(String handlerOpenid) {
        logger.info("获取处理人工单列表: handlerOpenid={}", handlerOpenid);
        // 获取当前处理人的工单
        List<WorkOrder> currentOrders = workOrderMapper.findByHandlerOpenid(handlerOpenid);
        
        // 获取处理人之前处理过但被重新分配的工单
        List<WorkOrder> previousOrders = workOrderMapper.findPreviouslyHandledOrders(handlerOpenid);
        
        // 合并两个列表
        List<WorkOrder> allOrders = new ArrayList<>();
        allOrders.addAll(currentOrders);
        allOrders.addAll(previousOrders);
        
        // 为每个工单添加用户信息
        for (WorkOrder order : allOrders) {
            User user = userService.getUserByOpenid(order.getUserOpenid());
            if (user != null) {
                order.setPhoneNumber(user.getPhoneNumber());
            }
        }
        
        return allOrders;
    }

    /**
     * 认领工单
     * @param workId 工单ID
     * @param handler 网格员用户名
     */
    @Transactional
    public void claimOrder(Integer workId, String handler) {
        logger.info("认领工单: workId={}, handlerOpenid={}", workId, handler);
        
        // 1. 获取工单
        WorkOrder workOrder = workOrderMapper.findById(workId);
        if (workOrder == null) {
            logger.error("认领工单失败: 工单不存在, workId={}", workId);
            throw new BusinessException("工单不存在");
        }
        
        // 2. 检查工单状态
        if (!"未领取".equals(workOrder.getStatus())) {
            logger.error("认领工单失败: 工单状态不正确, status={}", workOrder.getStatus());
            throw new BusinessException("只能认领未领取状态的工单");
        }
        
        // 3. 更新工单状态
        LocalDateTime now = LocalDateTime.now();
        workOrder.setStatus("处理中");
        workOrder.setHandledBy(handler);
        workOrder.setUpdatedAt(now); // 更新时间即为认领时间
        
        try {
            workOrderMapper.update(workOrder);
            logger.info("工单状态更新成功: workId={}, claimTime={}", workId, now);
            
            // 4. 记录处理日志
            WorkOrderProcessing processing = new WorkOrderProcessing();
            processing.setWorkId(workId);
            processing.setOperatorOpenid(handler);
            processing.setOperatorRole("网格员");
            processing.setActionType("认领工单");
            processing.setActionDescription("网格员认领工单");
            processing.setActionTime(now);
            processingMapper.insert(processing);
            
        } catch (Exception e) {
            logger.error("认领工单失败", e);
            throw new BusinessException("认领工单失败：" + e.getMessage());
        }
    }

    /**
     * 网格员提交工单反馈
     * @param workId 工单ID
     * @param handlerOpenid 处理人openid
     * @param handledDesc 处理说明
     * @param handledImages 处理图片URLs
     * @return 更新后的工单
     */
    @Transactional
    public WorkOrder submitWorkOrderFeedback(Integer workId, String handlerOpenid, String handledDesc, List<String> handledImages) {
        WorkOrder workOrder = findById(workId);
        if (workOrder == null) {
            throw new BusinessException("工单不存在");
        }
        if (!handlerOpenid.equals(workOrder.getHandledBy())) {
            throw new BusinessException("只有认领该工单的网格员才能提交反馈");
        }
        // 更新工单状态为"处理完"
        workOrder.setStatus("处理完");
        workOrder.setHandledDesc(handledDesc);
        workOrder.setHandledImages(handledImages);
        workOrder.setFeedbackTime(LocalDateTime.now());
        workOrderMapper.update(workOrder);

        // 插入处理日志
        WorkOrderProcessing processing = new WorkOrderProcessing();
        processing.setWorkId(workId);
        processing.setOperatorOpenid(handlerOpenid);
        processing.setOperatorRole("网格员");
        processing.setActionType("反馈处理");
        processing.setActionDescription("网格员提交反馈");
        processing.setActionTime(LocalDateTime.now());
        processingMapper.insert(processing);

        // 插入反馈记录
        WorkOrderFeedback feedback = new WorkOrderFeedback();
        feedback.setWorkId(workId);
        feedback.setHandlerOpenid(handlerOpenid);
        feedback.setHandlerRole("网格员");
        feedback.setFeedbackDescription(handledDesc);
        feedback.setFeedbackImages(handledImages);
        feedback.setFeedbackTime(LocalDateTime.now());
        feedbackMapper.insert(feedback);

        return workOrder;
    }

    /**
     * 获取片区长管理的工单列表
     * @param captainId 片区长ID
     * @param type 工单类型：all-全部，today-今日提交，processing-处理中，reported-已上报，completed-处理完
     * @return 工单列表
     */
    public List<WorkOrder> getCaptainWorkOrders(Integer captainId, String type) {
        switch (type) {
            case "today":
                // 获取今日提交的工单
                return workOrderMapper.findTodayUnclaimedOrders();
            case "processing":
                // 获取处理中的工单
                return workOrderMapper.findByStatus("处理中");
            case "reported":
                // 获取全部上报工单（未领取、已上报、处理中）
                return workOrderMapper.findReportedAndUnclaimedAndProcessing();
            case "completed":
                // 获取已完成的工单
                return workOrderMapper.findByStatus("处理完");
            default:
                // 获取所有工单
                return workOrderMapper.findAll();
        }
    }

    /**
     * 重新分配工单
     * @param workId 工单ID
     * @param gridWorkerOpenid 网格员openid
     * @param deadline 截止时间
     * @param captainOpenid 片区长openid
     */
    public void reassignWorkOrder(Integer workId, String gridWorkerOpenid, String deadline, String captainOpenid) {
        // 获取工单
        WorkOrder workOrder = workOrderMapper.findById(workId);
        if (workOrder == null) {
            throw new RuntimeException("工单不存在");
        }

        // 验证工单状态
        if (!"已上报".equals(workOrder.getStatus()) && !"未领取".equals(workOrder.getStatus())) {
            throw new RuntimeException("只能重新分配未领取或已上报状态的工单");
        }

        // 设置截止时间（如果未指定，默认为24小时后）
        LocalDateTime deadlineTime;
        if (deadline != null && !deadline.isEmpty()) {
            if (deadline.length() == 10) {
                // yyyy-MM-dd -> 补全为 当天结束的最后一秒 (23:59:59)
                deadlineTime = LocalDate.parse(deadline).plusDays(1).atStartOfDay().minusSeconds(1);
            } else {
                // 其它格式直接尝试 LocalDateTime 解析
                deadlineTime = LocalDateTime.parse(deadline);
            }
        } else {
            deadlineTime = LocalDateTime.now().plusHours(24);
        }

        // 更新工单状态和处理人
        workOrderMapper.updateStatus(workId, "处理中", gridWorkerOpenid);
        workOrderMapper.updateDeadline(workId, deadlineTime);

        // 记录重新分配日志
        WorkOrderProcessing processing = new WorkOrderProcessing();
        processing.setWorkId(workId);
        processing.setOperatorOpenid(captainOpenid); // 使用传入的片区长openid
        processing.setOperatorRole("片区长");
        processing.setActionType("片区长重新分配");
        // 获取网格员名称
        User gridWorker = userService.getUserByOpenid(gridWorkerOpenid);
        String gridWorkerName = (gridWorker != null && gridWorker.getUsername() != null) ? gridWorker.getUsername() : gridWorkerOpenid;

        // 格式化截止时间
        String formattedDeadlineTime = deadlineTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH时mm分"));

        processing.setActionDescription("将工单重新分配给网格员 " + gridWorkerName + ", 截止时间: " + formattedDeadlineTime);
        processing.setActionTime(LocalDateTime.now());
        processingMapper.insert(processing);
    }

    /**
     * 处理当天创建但未认领的超时工单
     */
    @Transactional
    public void reportUnclaimedTimeoutOrders() {
        logger.info("开始检查并上报当天创建但未认领的超时工单");
        LocalDateTime endOfToday = LocalDate.now().plusDays(1).atStartOfDay().minusSeconds(1); // 当天结束的最后一秒
        List<WorkOrder> timeoutOrders = workOrderMapper.findUnclaimedTimeoutOrdersToday(endOfToday);

        for (WorkOrder order : timeoutOrders) {
            try {
                // 更新工单状态为"已上报"
                order.setStatus("已上报");
                order.setUpdatedAt(LocalDateTime.now());
                workOrderMapper.update(order);

                // 记录系统超时上报日志
                WorkOrderProcessing processing = new WorkOrderProcessing();
                processing.setWorkId(order.getWorkId());
                processing.setOperatorOpenid("system"); // 系统操作标识
                processing.setOperatorRole("系统");
                processing.setActionType("系统超时上报");
                processing.setActionDescription("工单创建后当天未认领，系统自动上报");
                processing.setActionTime(LocalDateTime.now());
                processingMapper.insert(processing);
                logger.info("成功上报未认领超时工单: workId={}", order.getWorkId());
            } catch (Exception e) {
                logger.error("处理未认领超时工单失败: workId={}", order.getWorkId(), e);
            }
        }
         logger.info("当天创建但未认领的超时工单检查完成，共处理 {} 条", timeoutOrders.size());
    }

    /**
     * 处理认领后超时未完成的工单（无截止时间）
     */
    @Transactional
    public void reportProcessingTimeoutOrders() {
        logger.info("开始检查并上报认领后超时未完成的工单（无截止时间）");
        // 认领后超过24小时未完成，以 updated_at 为准
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(24);
        List<WorkOrder> timeoutOrders = workOrderMapper.findProcessingTimeoutOrdersWithoutDeadline(timeoutThreshold);

        for (WorkOrder order : timeoutOrders) {
            try {
                // 更新工单状态为"已上报"
                order.setStatus("已上报");
                order.setUpdatedAt(LocalDateTime.now());
                workOrderMapper.update(order);

                // 记录系统超时上报日志
                WorkOrderProcessing processing = new WorkOrderProcessing();
                processing.setWorkId(order.getWorkId());
                processing.setOperatorOpenid("system"); // 系统操作标识
                processing.setOperatorRole("系统");
                processing.setActionType("系统超时上报");
                processing.setActionDescription("工单认领后超过24小时未完成，系统自动上报");
                processing.setActionTime(LocalDateTime.now());
                processingMapper.insert(processing);
                logger.info("成功上报认领后超时工单: workId={}", order.getWorkId());
            } catch (Exception e) {
                logger.error("处理认领后超时工单失败: workId={}", order.getWorkId(), e);
            }
        }
        logger.info("认领后超时未完成的工单（无截止时间）检查完成，共处理 {} 条", timeoutOrders.size());
    }

    /**
     * 处理有截止时间但超期未完成的工单
     */
    @Transactional
    public void reportDeadlineTimeoutOrders() {
         logger.info("开始检查并上报有截止时间但超期未完成的工单");
         LocalDateTime currentTime = LocalDateTime.now();
         List<WorkOrder> timeoutOrders = workOrderMapper.findDeadlineTimeoutOrders(currentTime);

         for (WorkOrder order : timeoutOrders) {
            try {
                // 更新工单状态为"已上报"
                order.setStatus("已上报");
                order.setUpdatedAt(LocalDateTime.now());
                workOrderMapper.update(order);

                // 记录系统超时上报日志
                WorkOrderProcessing processing = new WorkOrderProcessing();
                processing.setWorkId(order.getWorkId());
                processing.setOperatorOpenid("system"); // 系统操作标识
                processing.setOperatorRole("系统");
                processing.setActionType("系统超时上报");
                processing.setActionDescription("工单超过截止时间未完成，系统自动上报");
                processing.setActionTime(LocalDateTime.now());
                processingMapper.insert(processing);
                logger.info("成功上报超期未完成工单: workId={}", order.getWorkId());
            } catch (Exception e) {
                logger.error("处理超期未完成工单失败: workId={}", order.getWorkId(), e);
            }
        }
         logger.info("有截止时间但超期未完成的工单检查完成，共处理 {} 条", timeoutOrders.size());
    }
} 