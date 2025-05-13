package com.reports.aipbackend.mapper;

import com.reports.aipbackend.entity.WorkOrderProcessing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkOrderProcessingMapper {
    // 根据日志ID查询处理记录
    WorkOrderProcessing findById(Long logId);
    
    // 根据工单ID查询处理记录
    List<WorkOrderProcessing> findByWorkId(Integer workId);
    
    // 根据操作人openid查询处理记录
    List<WorkOrderProcessing> findByOperatorOpenid(String operatorOpenid);
    
    // 根据操作类型查询处理记录
    List<WorkOrderProcessing> findByActionType(String actionType);
    
    // 插入处理记录
    int insert(WorkOrderProcessing processing);
} 