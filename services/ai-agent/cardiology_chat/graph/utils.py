"""Graph 层公共工具（唯一入口，节点只从这里 import）。

分区说明：
  1. 消息读取   — 从 CardiologyState 提取用户/会话文本
  2. 规则检测   — 关键词命中、否定过滤
  3. JSON 解析  — 清洗 LLM 返回的 markdown 代码块
  4. LLM 调用   — Flash 缓存、invoke、字段校验与兜底、标准输出组装
"""

import json
import re

from langchain_core.messages import HumanMessage, SystemMessage

from cardiology_chat.factory.LLMFactory import LLMModelFactory
from cardiology_chat.graph.state import CardiologyState

# ---------------------------------------------------------------------------
# 1. 消息读取
# ---------------------------------------------------------------------------

def latest_user_message(state: CardiologyState) -> str:
    """取最近一条用户消息文本。"""
    for message in reversed(state.get("messages", [])):
        if isinstance(message, HumanMessage):
            content = message.content
            if isinstance(content, str):
                return content.strip()
    return ""


def conversation_messages_for_llm(state: CardiologyState, max_messages: int = 10) -> list:
    """取最近 N 条消息，供多轮 LLM 上下文使用。"""
    messages = state.get("messages", [])
    return messages[-max_messages:] if messages else []


def all_user_text(state: CardiologyState) -> str:
    """拼接全部用户消息，供跨轮关键词检测使用。"""
    parts = []
    for message in state.get("messages", []):
        if isinstance(message, HumanMessage):
            content = message.content
            if isinstance(content, str) and content.strip():
                parts.append(content.strip())
    return "\n".join(parts)


# ---------------------------------------------------------------------------
# 2. 规则检测
# ---------------------------------------------------------------------------

_NEGATION_PREFIXES = ("没有", "无", "不", "未")


def has_keyword(
    text: str,
    keywords: tuple[str, ...],
    *,
    case_insensitive: bool = False,
    negation_aware: bool = True,
) -> bool:
    """检查文本是否命中关键词。

    negation_aware=True 时，关键词前 4 字含否定词则不算命中（如「没有大汗」）。
    """
    haystack = text.lower() if case_insensitive else text
    for keyword in keywords:
        needle = keyword.lower() if case_insensitive else keyword
        if needle not in haystack:
            continue
        if negation_aware:
            idx = haystack.find(needle)
            window = haystack[max(0, idx - 4):idx]
            if any(neg in window for neg in _NEGATION_PREFIXES):
                continue
        return True
    return False


# ---------------------------------------------------------------------------
# 3. JSON 解析
# ---------------------------------------------------------------------------

def parse_llm_json(text: str) -> dict:
    """解析 LLM 返回的 JSON，自动剥离 ```json 代码块。"""
    if not text:
        return {}

    cleaned = text.strip()
    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
        cleaned = re.sub(r"\s*```$", "", cleaned)
    try:
        data = json.loads(cleaned)
        return data if isinstance(data, dict) else {}
    except json.JSONDecodeError:
        return {}


# ---------------------------------------------------------------------------
# 4. LLM 调用
# ---------------------------------------------------------------------------

STANDARD_LLM_KEYS = ("urgency", "explanation", "advice", "disclaimer")

_flash_llm_cache: dict[float, object] = {}


def get_flash_llm(temperature: float = 0.3):
    """按 temperature 缓存 Flash 实例，避免重复创建。"""
    if temperature not in _flash_llm_cache:
        _flash_llm_cache[temperature] = LLMModelFactory().Get_LLM_Model(temperature=temperature)
    return _flash_llm_cache[temperature]


def is_valid_llm_data(data: dict, required_keys: tuple[str, ...] = STANDARD_LLM_KEYS) -> bool:
    """校验 LLM JSON 是否包含所有必填非空字符串字段。"""
    return all(
        isinstance(data.get(key), str) and data.get(key).strip()
        for key in required_keys
    )


def invoke_llm_json(
    state: CardiologyState,
    system_prompt: str,
    *,
    required_keys: tuple[str, ...] = STANDARD_LLM_KEYS,
    temperature: float = 0.3,
    user_text: str | None = None,
) -> dict:
    """调用 Flash 并解析 JSON；校验失败或异常时返回空 dict。"""
    try:
        llm = get_flash_llm(temperature)
        messages = [SystemMessage(content=system_prompt)]
        if user_text is not None:
            messages.append(HumanMessage(content=user_text))
        else:
            messages.extend(conversation_messages_for_llm(state))
        response = llm.invoke(messages)
        data = parse_llm_json(response.content)
        return data if is_valid_llm_data(data, required_keys) else {}
    except Exception:
        return {}


def pick_llm_field(data: dict, key: str, fallback: str) -> str:
    """取 LLM 字段值，空或非法时返回 fallback。"""
    value = data.get(key)
    return value if isinstance(value, str) and value.strip() else fallback


def build_standard_llm_fields(
    llm_data: dict | None,
    *,
    triage_fallback: str,
    impression_fallback: str,
    advice_fallback: str,
    disclaimer_fallback: str,
) -> dict:
    """把 Flash 四个标准 JSON 字段映射为节点 state 写入字段。"""
    data = llm_data or {}
    return {
        "triage_level": pick_llm_field(data, "urgency", triage_fallback),
        "clinical_impression": pick_llm_field(data, "explanation", impression_fallback),
        "management_advice": pick_llm_field(data, "advice", advice_fallback),
        "medical_disclaimer": pick_llm_field(data, "disclaimer", disclaimer_fallback),
    }
