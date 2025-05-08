-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    openid VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    phone_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建工单表
CREATE TABLE work_orders (
    work_id INT AUTO_INCREMENT PRIMARY KEY, -- 工单ID
    user_openid VARCHAR(255) NOT NULL, -- 提交用户的openid
    title VARCHAR(255), -- 工单标题（可选）
    description TEXT NOT NULL, -- 问题描述
    image_urls TEXT, -- 图片链接，可以存储JSON格式数组
    address VARCHAR(255), -- 地址描述
    building_info VARCHAR(255), -- 楼栋号
    status ENUM('未领取', '处理中','已上报', '处理完') DEFAULT '未领取', -- 工单状态
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间
    handled_by VARCHAR(255), -- 处理人的openid（网格员或片区长）
    handled_images TEXT, -- 处理后的图片链接，同样可以是JSON格式数组
    handled_desc TEXT, -- 处理说明
    feedback_time TIMESTAMP, -- 反馈时间
    FOREIGN KEY (user_openid) REFERENCES users(openid),
    FOREIGN KEY (handled_by) REFERENCES users(openid)
);


-- 创建工单处理记录表
CREATE TABLE work_order_processing (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 日志ID
    work_id INT NOT NULL, -- 对应的工单ID
    operator_openid VARCHAR(255) NOT NULL, -- 操作人openid
    operator_role ENUM('普通用户', '网格员', '片区长') NOT NULL, -- 操作人角色
    action_type ENUM(
        '提交工单', -- 普通用户提交工单
        '认领工单', -- 网格员认领工单
        '开始处理', -- 网格员开始处理工单
        '反馈处理', -- 网格员反馈处理结果
        '主动上报', -- 网格员主动上报给片区长
        '系统超时上报', -- 系统自动超时上报
        '片区长处理' -- 片区长处理
    ) NOT NULL, -- 操作类型
    action_description TEXT, -- 操作描述（可选）
    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 操作时间
    extra_data JSON, -- 额外信息（如图片URL数组、定位等）
    FOREIGN KEY (report_id) REFERENCES reports(report_id),
    FOREIGN KEY (operator_openid) REFERENCES users(openid)
);

-- 创建工单反馈表
CREATE TABLE IF NOT EXISTS work_order_feedback (
    feedback_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    work_id INT NOT NULL,
    handler_openid VARCHAR(100) NOT NULL,
    handler_role VARCHAR(20) NOT NULL,
    feedback_description TEXT,
    feedback_images TEXT,
    feedback_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (work_id) REFERENCES work_orders(work_id)
);

-- 插入测试用户数据
INSERT INTO users (username, password_hash, role) VALUES 
('rsc', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '片区长')
ON DUPLICATE KEY UPDATE username=username; 