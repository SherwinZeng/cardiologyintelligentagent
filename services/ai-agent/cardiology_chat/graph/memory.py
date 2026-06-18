"""Conversation memory extraction for the graph entry path.

The graph is invoked from a fresh state on every request, while Java supplies a
recent message window. This node turns that transcript window into explicit
state fields before routing, so stable conversational facts are not left to LLM
recall or fallback behavior.
"""

import re

from langchain_core.messages import HumanMessage

from cardiology_chat.graph.llm.messages import latest_user_message
from cardiology_chat.graph.state import CardiologyState

_NAME_INTRO_PATTERNS = (
    re.compile(r"(?:我叫|我的名字(?:是|叫)|叫我)(?P<name>[^\s，,。！？!?；;：:、]{1,16})"),
    re.compile(r"我是(?P<name>[^\s，,。！？!?；;：:、]{1,16})"),
)

_NAME_RECALL_MARKERS = (
    "我叫什么",
    "我叫啥",
    "我的名字",
    "知道我叫",
    "知道我叫什么",
    "记得我叫",
    "记得我的名字",
    "还记得我",
    "你怎么称呼我",
    "怎么称呼我",
    "怎么叫我",
    "叫我什么",
    "你叫我什么",
    "该叫我什么",
)

_TRAILING_PARTICLES = "呀啊呢哦哈啦嘛吧了吖哇"
_NAME_STOP_WORDS = ("今年", "年龄", "来自", "是个", "是一个")
_INVALID_NAME_PREFIXES = ("什么", "啥", "谁", "哪个", "哪位", "名字", "称呼")


def conversation_memory_node(state: CardiologyState) -> dict:
    """Recover deterministic conversation facts from the message window."""
    user_display_name = _extract_user_display_name(state)
    memory = {}
    if user_display_name:
        memory["user_display_name"] = user_display_name

    return {
        "user_display_name": user_display_name,
        "conversation_memory": memory,
    }


def is_user_name_recall(text: str) -> bool:
    """Whether the latest user message is asking the assistant to recall their name."""
    return any(marker in text for marker in _NAME_RECALL_MARKERS)


def _extract_user_display_name(state: CardiologyState) -> str:
    latest = latest_user_message(state)
    user_texts = [
        message.content.strip()
        for message in state.get("messages", [])
        if isinstance(message, HumanMessage)
        and isinstance(message.content, str)
        and message.content.strip()
    ]

    for text in reversed(user_texts):
        if text == latest and is_user_name_recall(latest):
            continue
        if is_user_name_recall(text):
            continue
        name = _extract_name_from_text(text)
        if name:
            return name
    return ""


def _extract_name_from_text(text: str) -> str:
    for pattern in _NAME_INTRO_PATTERNS:
        match = pattern.search(text)
        if not match:
            continue
        name = _clean_name(match.group("name"))
        if name:
            return name
    return ""


def _clean_name(name: str) -> str:
    value = name.strip().strip(_TRAILING_PARTICLES)
    for stop_word in _NAME_STOP_WORDS:
        if stop_word in value:
            value = value.split(stop_word, 1)[0]
    value = value.strip().strip(_TRAILING_PARTICLES)
    if value.startswith(_INVALID_NAME_PREFIXES):
        return ""
    if 1 <= len(value) <= 12:
        return value
    return ""
