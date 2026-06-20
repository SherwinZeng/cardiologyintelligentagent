USE cardiology;

ALTER TABLE chat_message
  ADD COLUMN guide_references TEXT DEFAULT NULL COMMENT '参考指南中文名 JSON 数组' AFTER disclaimer;
