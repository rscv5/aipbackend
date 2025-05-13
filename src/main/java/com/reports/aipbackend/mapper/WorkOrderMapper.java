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
} 