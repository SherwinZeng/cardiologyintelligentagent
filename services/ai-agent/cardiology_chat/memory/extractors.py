"""兼容旧测试/旧调用方的轻量 extractors。

正式逻辑在 graph.dialogue_core.MemoryExtractor；这里保留小函数，方便你单独练习。
"""

from cardiology_chat.graph.dialogue_core import MemoryExtractor
from cardiology_chat.graph.state import empty_cardiology_state


def extract_display_name(text: str) -> str | None:
    updates = MemoryExtractor.commit(empty_cardiology_state(), text)
    return updates.get("user_display_name")


def extract_age(text: str) -> str | None:
    memory = MemoryExtractor.commit(empty_cardiology_state(), text).get("conversation_memory") or {}
    return memory.get("age")


def extract_sex(text: str) -> str | None:
    memory = MemoryExtractor.commit(empty_cardiology_state(), text).get("conversation_memory") or {}
    return memory.get("sex")
