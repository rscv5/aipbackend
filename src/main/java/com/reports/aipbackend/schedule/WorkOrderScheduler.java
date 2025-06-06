package com.reports.aipbackend.schedule;

import com.reports.aipbackend.entity.WorkOrder;
import com.reports.aipbackend.service.WorkOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// 工单调度器
@Component
public class WorkOrderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderScheduler.class);

    @Autowired
    private WorkOrderService workOrderService;


   // 定时任务：应用启动后延迟5秒执行，之后每1小时执行依次（仅用于测试）
   //@Scheduled(fixedDelay = 3600000, initialDelay = 5000)
    // 定时任务：每天凌晨1点执行，检查超时未认领和超期未完成的工单
    @Scheduled(cron = "0 0 1 * * ?")
    public void reportTimeoutWorkOrders() {
        logger.info("定时任务开始：检查超时工单");
        try {
            // 1. 检查当天未认领的工单（创建时间在昨天或更早且状态为未领取）
            workOrderService.reportUnclaimedTimeoutOrders();

            // 2. 检查认领后超时未完成的工单（认领后超过24小时且无截止时间）
            workOrderService.reportProcessingTimeoutOrders();

            // 3. 检查有截止时间但超期未完成的工单
            workOrderService.reportDeadlineTimeoutOrders();

            logger.info("定时任务结束：超时工单检查完成");
        } catch (Exception e) {
            logger.error("定时任务执行失败", e);
        }
    }
} 