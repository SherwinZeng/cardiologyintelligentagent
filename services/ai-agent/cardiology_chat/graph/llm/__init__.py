"""Graph 层 LLM 与消息工具。

节点开发时统一从这里 import，不要直接依赖子模块路径。

子模块：
  messages.py         — 从 state 提取用户/会话文本
  keywords.py         — 关键词命中（含否定词过滤）
  json_parser.py      — 清洗 LLM 返回的 JSON
  invoke.py           — 调 Flash、重试、映射 state 字段
  conversation_node.py — 多轮节点统一入口
"""

from cardiology_chat.graph.llm.invoke import (
    STANDARD_LLM_KEYS,
    build_standard_llm_fields,
    get_flash_llm,
    invoke_llm_json,
    invoke_llm_json_with_retry,
    is_valid_llm_data,
    pick_llm_field,
)
from cardiology_chat.graph.llm.keywords import has_keyword
from cardiology_chat.graph.llm.messages import (
    all_user_text,
    conversation_messages_for_llm,
    latest_user_message,
)

__all__ = [
    "STANDARD_LLM_KEYS",
    "all_user_text",
    "build_standard_llm_fields",
    "conversation_messages_for_llm",
    "get_flash_llm",
    "has_keyword",
    "invoke_llm_json",
    "invoke_llm_json_with_retry",
    "is_valid_llm_data",
    "latest_user_message",
    "pick_llm_field",
]
