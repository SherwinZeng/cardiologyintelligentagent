USE cardiology;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id VARCHAR(128) NOT NULL COMMENT '会话 ID',
  uid VARCHAR(128) NOT NULL COMMENT '用户 ID',
  role VARCHAR(32) NOT NULL COMMENT 'user / assistant',
  content TEXT NOT NULL COMMENT '消息正文',
  urgency VARCHAR(16) DEFAULT NULL COMMENT '分诊级别 green/yellow/red',
  explanation TEXT DEFAULT NULL COMMENT '临床印象',
  advice TEXT DEFAULT NULL COMMENT '处理建议',
  disclaimer TEXT DEFAULT NULL COMMENT '免责声明',
  guide_references TEXT DEFAULT NULL COMMENT '参考指南中文名 JSON 数组',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_chat_message_session (uid, session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
