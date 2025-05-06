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
CREATE TABLE IF NOT EXISTS work_orders (
    work_id INT PRIMARY KEY AUTO_INCREMENT,
    user_openid VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    image_urls TEXT,
    location VARCHAR(200),
    building_info VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT '待处理',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    handled_by VARCHAR(100),
    handled_images TEXT,
    handled_desc TEXT,
    feedback_time TIMESTAMP
);

-- 创建工单处理记录表
CREATE TABLE IF NOT EXISTS work_order_processing (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    work_id INT NOT NULL,
    operator_openid VARCHAR(100) NOT NULL,
    operator_role VARCHAR(20) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_description TEXT,
    extra_data TEXT,
    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (work_id) REFERENCES work_orders(work_id)
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