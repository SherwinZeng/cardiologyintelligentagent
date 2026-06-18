"""从 CardiologyState 读取对话消息。"""

from langchain_core.messages import AIMessage, HumanMessage

from cardiology_chat.graph.state import CardiologyState

# 传给 LLM 的 assistant 历史上限（完整内容仍存 Java/Redis，此处仅截断 prompt）
ASSISTANT_LLM_MAX_CHARS = 480

def latest_user_message(state: CardiologyState) -> str:
    """取最近一条用户消息（HumanMessage）文本。"""
    for message in reversed(state.get("messages", [])):
        if isinstance(message, HumanMessage):
            content = message.content
            if isinstance(content, str):
                return content.strip()
    return ""


def _truncate_assistant_content(content: str, max_chars: int = ASSISTANT_LLM_MAX_CHARS) -> str:
    if len(content) <= max_chars:
        return content
    return content[: max_chars - 1].rstrip() + "…"


def conversation_messages_for_llm(state: CardiologyState, max_messages: int = 12) -> list:
    """取最近 N 条消息；user 原文保留，assistant 截断以免长回复淹没上下文。"""
    raw = state.get("messages", [])[-max_messages:] if state.get("messages") else []
    result = []
    for message in raw:
        if isinstance(message, HumanMessage):
            result.append(message)
            continue
        if isinstance(message, AIMessage):
            content = message.content
            text = content if isinstance(content, str) else str(content)
            result.append(AIMessage(content=_truncate_assistant_content(text)))
            continue
        result.append(message)
    return result


def all_user_text(state: CardiologyState) -> str:
    """拼接本轮 thread 内全部用户消息，用于跨轮关键词检测（如既往史提取）。"""
    parts = []
    for message in state.get("messages", []):
        if isinstance(message, HumanMessage):
            content = message.content
            if isinstance(content, str) and content.strip():
                parts.append(content.strip())
    return "\n".join(parts)
