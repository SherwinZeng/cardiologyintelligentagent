"""兼容旧调用方的记忆提交入口。"""

from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.graph.dialogue_core import MemoryExtractor


def commit_user_facts(state: CardiologyState, user_text: str) -> dict:
    return MemoryExtractor.commit(state, user_text)
