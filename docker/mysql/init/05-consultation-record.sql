USE cardiology;

CREATE TABLE IF NOT EXISTS consultation_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '问诊记录 ID',
  session_id VARCHAR(128) NOT NULL COMMENT '来源会话 ID',
  uid VARCHAR(128) NOT NULL COMMENT '归属用户 uid',
  title VARCHAR(32) NOT NULL DEFAULT '问诊记录' COMMENT 'AI 生成的短标题，不超过 6 个汉字',
  urgency VARCHAR(16) NOT NULL DEFAULT 'green' COMMENT '最高紧急程度 green / yellow / red',
  summary VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '问诊摘要，用于记录列表展示',
  message_count INT NOT NULL DEFAULT 0 COMMENT '生成摘要时的消息条数',
  started_at DATETIME DEFAULT NULL COMMENT '会话开始时间',
  ended_at DATETIME DEFAULT NULL COMMENT '会话最近活跃时间',
  generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '摘要生成时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_consultation_record_session (session_id),
  KEY idx_consultation_record_uid_ended (uid, ended_at),
  KEY idx_consultation_record_uid_urgency (uid, urgency, ended_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
