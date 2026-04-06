-- 资产表
CREATE TABLE IF NOT EXISTS `asset` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `asset_id` VARCHAR(32) NOT NULL UNIQUE COMMENT '资产ID',
    `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
    `asset_type` INT NOT NULL COMMENT '资产类型: 1现金 2储蓄卡 3信用卡 4虚拟账户 5投资账户 6负债 7债权 8自定义',
    `name` VARCHAR(100) NOT NULL COMMENT '资产名称',
    `bank_name` VARCHAR(50) DEFAULT NULL COMMENT '银行名称(银行卡类专用)',
    `card_last_four` VARCHAR(4) DEFAULT NULL COMMENT '卡号后四位(银行卡类专用)',
    `balance` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '余额/欠款金额',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_asset_type` (`asset_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产账户表';

-- 资产变动记录表
CREATE TABLE IF NOT EXISTS `asset_record` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `record_id` VARCHAR(32) NOT NULL UNIQUE COMMENT '记录ID',
    `asset_id` VARCHAR(32) NOT NULL COMMENT '资产ID',
    `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
    `operation_type` INT NOT NULL COMMENT '操作类型: 1手动调整 2支出 3收入 4转入 5转出',
    `before_balance` DECIMAL(15,2) NOT NULL COMMENT '变动前余额',
    `after_balance` DECIMAL(15,2) NOT NULL COMMENT '变动后余额',
    `change_amount` DECIMAL(15,2) NOT NULL COMMENT '变动金额',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_asset_id` (`asset_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产变动记录表';
