-- 为 activity_user_rel 表添加 nickname 字段，用于存储用户在账本内的自定义昵称
ALTER TABLE activity_user_rel ADD COLUMN nickname VARCHAR(50) DEFAULT NULL COMMENT '账本内昵称';
