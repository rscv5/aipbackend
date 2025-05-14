package com.reports.aipbackend.mapper;

import com.reports.aipbackend.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
} 