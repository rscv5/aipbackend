package com.reports.aipbackend.mapper;

import com.reports.aipbackend.entity.WorkOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkOrderMapper {
    // 根据工单ID查询工单
    @Select("SELECT * FROM work_orders WHERE work_id = #{workId}")
    WorkOrder findById(Integer workId);
    
    // 根据用户openid查询工单列表
    @Select("SELECT * FROM work_orders WHERE user_openid = #{userOpenid}")
    List<WorkOrder> findByUserOpenid(String userOpenid);
    
    // 根据处理人openid查询工单列表
    @Select("SELECT * FROM work_orders WHERE handled_by = #{handledBy}")
    List<WorkOrder> findByHandlerOpenid(String handledBy);
    
    // 根据状态查询工单列表
    @Select("SELECT * FROM work_orders WHERE status = #{status}")
    List<WorkOrder> findByStatus(String status);
    
    // 创建工单
    @Insert("INSERT INTO work_orders (user_openid, title, description, image_urls, location, building_info, status) " +
            "VALUES (#{userOpenid}, #{title}, #{description}, #{imageUrls}, #{location}, #{buildingInfo}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "workId")
    int insert(WorkOrder workOrder);
    
    // 更新工单状态和处理信息
    @Update("UPDATE work_orders SET status = #{status}, handled_by = #{handledBy}, " +
            "handled_images = #{handledImages}, handled_desc = #{handledDesc}, " +
            "feedback_time = #{feedbackTime} WHERE work_id = #{workId}")
    int update(WorkOrder workOrder);
    
    // 获取所有工单
    @Select("SELECT * FROM work_orders ORDER BY created_at DESC")
    List<WorkOrder> findAll();
} 