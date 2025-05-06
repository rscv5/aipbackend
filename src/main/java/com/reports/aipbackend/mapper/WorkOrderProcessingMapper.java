package com.reports.aipbackend.mapper;

import com.reports.aipbackend.entity.WorkOrderProcessing;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkOrderProcessingMapper {
    // 根据日志ID查询处理记录
    @Select("SELECT * FROM work_order_processing WHERE log_id = #{logId}")
    WorkOrderProcessing findById(Long logId);
    
    // 根据工单ID查询处理记录
    @Select("SELECT * FROM work_order_processing WHERE work_id = #{workId} ORDER BY action_time DESC")
    List<WorkOrderProcessing> findByWorkId(Integer workId);
    
    // 根据操作人openid查询处理记录
    @Select("SELECT * FROM work_order_processing WHERE operator_openid = #{operatorOpenid} ORDER BY action_time DESC")
    List<WorkOrderProcessing> findByOperatorOpenid(String operatorOpenid);
    
    // 根据操作类型查询处理记录
    @Select("SELECT * FROM work_order_processing WHERE action_type = #{actionType} ORDER BY action_time DESC")
    List<WorkOrderProcessing> findByActionType(String actionType);
    
    // 插入处理记录
    @Insert("INSERT INTO work_order_processing (work_id, operator_openid, operator_role, action_type, " +
            "action_description, extra_data) VALUES (#{workId}, #{operatorOpenid}, #{operatorRole}, " +
            "#{actionType}, #{actionDescription}, #{extraData})")
    @Options(useGeneratedKeys = true, keyProperty = "logId")
    int insert(WorkOrderProcessing processing);
} 