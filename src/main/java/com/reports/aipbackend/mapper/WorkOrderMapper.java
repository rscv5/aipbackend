package com.reports.aipbackend.mapper;

import com.reports.aipbackend.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WorkOrderMapper {
    WorkOrder findById(Integer workId);

    List<WorkOrder> findByUserOpenid(String userOpenid);

    List<WorkOrder> findByHandlerOpenid(String handledBy);

    List<WorkOrder> findByStatus(String status);

    int insert(WorkOrder workOrder);

    int update(WorkOrder workOrder);

    List<WorkOrder> findAll();

    List<WorkOrder> findByUserOpenidAndStatus(@Param("userOpenid") String userOpenid,
                                              @Param("status") String status,
                                              @Param("statusList") List<String> statusList);

    List<WorkOrder> findRecentOrdersByUser(@Param("userOpenid") String userOpenid,
                                           @Param("startTime") LocalDateTime startTime);

    /**
     * 查询今日未领取的工单
     * @return 工单列表
     */
    List<WorkOrder> findTodayUnclaimedOrders();

    /**
     * 根据状态和时间范围查询工单
     * @param status 状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 工单列表
     */
    List<WorkOrder> findByStatusAndCreatedAtBetween(@Param("status") String status,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 根据处理人查询工单
     * @param handler 处理人
     * @return 工单列表
     */
    List<WorkOrder> findByHandler(String handler);

    /**
     * 查询处理人之前处理过但被重新分配的工单
     * @param handlerOpenid 处理人openid
     * @return 工单列表
     */
    List<WorkOrder> findPreviouslyHandledOrders(@Param("handlerOpenid") String handlerOpenid);

    /**
     * 更新工单状态和处理人
     * @param workId 工单ID
     * @param status 状态
     * @param handledBy 处理人openid
     */
    @Update("UPDATE work_orders SET status = #{status}, handled_by = #{handledBy}, updated_at = NOW() WHERE work_id = #{workId}")
    void updateStatus(@Param("workId") Integer workId,
                     @Param("status") String status,
                     @Param("handledBy") String handledBy);

    /**
     * 更新工单截止时间
     * @param workId 工单ID
     * @param deadline 截止时间
     */
    @Update("UPDATE work_orders SET deadline = #{deadline}, updated_at = NOW() " +
            "WHERE work_id = #{workId}")
    void updateDeadline(@Param("workId") Integer workId,
                       @Param("deadline") LocalDateTime deadline);

    /**
     * 查询所有未领取、已上报、处理中状态的工单
     */
    List<WorkOrder> findReportedAndUnclaimedAndProcessing();

    /**
     * 查询当天创建但未认领的工单
     * @param endOfToday 当天结束时间
     * @return 工单列表
     */
    List<WorkOrder> findUnclaimedTimeoutOrdersToday(@Param("endOfToday") LocalDateTime endOfToday);

    /**
     * 查询认领后超时未完成的工单（无截止时间）
     * @param timeoutThreshold 超时阈值时间
     * @return 工单列表
     */
    List<WorkOrder> findProcessingTimeoutOrdersWithoutDeadline(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * 查询有截止时间但超期未完成的工单
     * @param currentTime 当前时间
     * @return 工单列表
     */
    List<WorkOrder> findDeadlineTimeoutOrders(@Param("currentTime") LocalDateTime currentTime);
} 