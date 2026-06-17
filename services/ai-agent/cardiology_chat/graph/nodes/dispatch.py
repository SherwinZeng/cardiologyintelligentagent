from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.graph.utils import latest_user_message
from cardiology_chat.prompts.dispatch import (
    SYMPTOM_KEYWORDS,
    HISTORY_KEYWORDS,
    LAB_KEYWORDS,
    GREETING_KEYWORDS,
    META_KEYWORDS,
    CARDIO_SCOPE_KEYWORDS,
    MEDICATION_KEYWORDS,
)


def route_after_dispatch(state: CardiologyState) -> str:
    return state["route"]


def _contains_keyword(text: str, keywords: tuple[str, ...]) -> bool:
    """中文按原文包含；英文缩写等忽略大小写。"""
    lower = text.lower()
    for k in keywords:
        if k.isascii():
            if k.lower() in lower:
                return True
        elif k in text:
            return True
    return False


def _is_greeting_or_meta(text: str) -> bool:
    return _contains_keyword(text, GREETING_KEYWORDS) or _contains_keyword(text, META_KEYWORDS)


def clinical_dispatch_node(state: CardiologyState) -> dict:
    text = latest_user_message(state)

    if not text:
        return {"route": "fallback"}

    current_route = state.get("route")

    # 每轮先识别「显式意图切换」，避免锁在 symptom 无法问 ARB/CCB
    if _contains_keyword(text, LAB_KEYWORDS):
        return {"route": "lab"}

    if _contains_keyword(text, MEDICATION_KEYWORDS):
        return {"route": "medication"}

    if _contains_keyword(text, HISTORY_KEYWORDS):
        return {"route": "history"}

    if _contains_keyword(text, SYMPTOM_KEYWORDS):
        return {"route": "symptom"}

    if _is_greeting_or_meta(text):
        return {"route": "greeting"}

    if _contains_keyword(text, CARDIO_SCOPE_KEYWORDS):
        return {"route": "symptom"}

    # 本轮无明确切换意图时，才继续未完成的采集流程
    if not state.get("hpi_complete") and current_route == "symptom":
        return {"route": "symptom"}

    if not state.get("pmh_complete") and current_route == "history":
        return {"route": "history"}

    if current_route == "lab" and "待补充" in (state.get("investigation_summary") or ""):
        return {"route": "lab"}

    return {"route": "fallback"}
