-- 旧库增量迁移（新环境已由 02-chat-session.sql 包含，无需执行）
USE cardiology;

ALTER TABLE chat_session
  ADD COLUMN archived_at DATETIME DEFAULT NULL COMMENT '归档时间';

CREATE INDEX idx_chat_session_status_updated_at ON chat_session (status, updated_at);
CREATE INDEX idx_chat_session_status_archived_at ON chat_session (status, archived_at);
