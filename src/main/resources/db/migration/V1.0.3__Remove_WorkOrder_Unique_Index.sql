-- 查看外键约束
SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME = 'work_orders'
AND REFERENCED_COLUMN_NAME IN ('user_openid', 'address');

-- 删除外键约束（如果有的话）
SET FOREIGN_KEY_CHECKS = 0;

-- 删除 work_orders 表中的唯一索引
ALTER TABLE work_orders DROP INDEX idx_user_address_desc;

-- 添加普通索引用于查询优化
CREATE INDEX idx_user_created ON work_orders(user_openid, created_at);

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1; 