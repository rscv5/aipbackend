package com.reports.aipbackend.mapper;

import com.reports.aipbackend.entity.WorkOrderFeedback;
import java.util.List;

public interface WorkOrderFeedbackMapper {
    WorkOrderFeedback findById(Long feedbackId);
    List<WorkOrderFeedback> findByWorkId(Integer workId);
    List<WorkOrderFeedback> findByHandlerOpenid(String handlerOpenid);
    int insert(WorkOrderFeedback feedback);
    int update(WorkOrderFeedback feedback);
} 