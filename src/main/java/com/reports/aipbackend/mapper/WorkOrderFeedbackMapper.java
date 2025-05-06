package com.reports.aipbackend.mapper;

import com.reports.aipbackend.entity.WorkOrderFeedback;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkOrderFeedbackMapper {
    // 根据反馈ID查询反馈
    @Select("SELECT * FROM work_order_feedback WHERE feedback_id = #{feedbackId}")
    WorkOrderFeedback findById(Long feedbackId);
    
    // 根据工单ID查询反馈
    @Select("SELECT * FROM work_order_feedback WHERE work_id = #{workId} ORDER BY feedback_time DESC")
    List<WorkOrderFeedback> findByWorkId(Integer workId);
    
    // 根据处理人openid查询反馈
    @Select("SELECT * FROM work_order_feedback WHERE handler_openid = #{handlerOpenid} ORDER BY feedback_time DESC")
    List<WorkOrderFeedback> findByHandlerOpenid(String handlerOpenid);
    
    // 插入反馈
    @Insert("INSERT INTO work_order_feedback (work_id, handler_openid, handler_role, " +
            "feedback_description, feedback_images) VALUES (#{workId}, #{handlerOpenid}, " +
            "#{handlerRole}, #{feedbackDescription}, #{feedbackImages})")
    @Options(useGeneratedKeys = true, keyProperty = "feedbackId")
    int insert(WorkOrderFeedback feedback);
} 