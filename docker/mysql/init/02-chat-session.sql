USE cardiology;

CREATE TABLE IF NOT EXISTS chat_session (
  session_id VARCHAR(128) NOT NULL COMMENT '会话 ID，对应 LangGraph thread_id',
  uid VARCHAR(128) NOT NULL COMMENT '归属用户 uid',
  title VARCHAR(255) NOT NULL DEFAULT '新建会话' COMMENT '会话标题',
  preview VARCHAR(512) NOT NULL DEFAULT '' COMMENT '最近消息摘要',
  message_count INT NOT NULL DEFAULT 0 COMMENT '消息条数',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active / archived',
  summary_status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '问诊总结状态：pending / processing / done / failed',
  summary_generated_at DATETIME DEFAULT NULL COMMENT '最近一次问诊总结生成时间',
  summary_retry_count INT NOT NULL DEFAULT 0 COMMENT '问诊总结失败重试次数',
  summary_error VARCHAR(512) DEFAULT NULL COMMENT '最近一次问诊总结失败原因',
  summary_attempted_at DATETIME DEFAULT NULL COMMENT '最近一次问诊总结尝试时间',
  pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  pinned_at DATETIME DEFAULT NULL COMMENT '置顶时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近活跃时间',
  archived_at DATETIME DEFAULT NULL COMMENT '归档时间',
  PRIMARY KEY (session_id),
  KEY idx_chat_session_uid_updated (uid, updated_at),
  KEY idx_chat_session_status_updated_at (status, updated_at),
  KEY idx_chat_session_status_archived_at (status, archived_at),
  KEY idx_chat_session_summary_scan (status, summary_status, updated_at, message_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
