-- 旧库增量迁移（新环境已由 02-chat-session.sql 包含，无需执行）
USE cardiology;

ALTER TABLE chat_session
  ADD COLUMN pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  ADD COLUMN pinned_at DATETIME DEFAULT NULL COMMENT '置顶时间';
