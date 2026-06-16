from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.graph.utils import latest_user_message
from cardiology_chat.prompts.dispatch import (
    SYMPTOM_KEYWORDS,
    HISTORY_KEYWORDS,
    LAB_KEYWORDS,
    GREETING_KEYWORDS,
    META_KEYWORDS,
    CARDIO_SCOPE_KEYWORDS,
)


def route_after_dispatch(state: CardiologyState) -> str:
    return state["route"]


def _is_greeting_or_meta(text: str) -> bool:
    return any(k in text for k in GREETING_KEYWORDS) or any(k in text for k in META_KEYWORDS)


def clinical_dispatch_node(state: CardiologyState) -> dict:
    current_route = state.get("route")

    if not state.get("hpi_complete") and current_route == "symptom":
        return {"route": "symptom"}

    if not state.get("pmh_complete") and current_route == "history":
        return {"route": "history"}

    if current_route == "lab" and "待补充" in (state.get("investigation_summary") or ""):
        return {"route": "lab"}

    text = latest_user_message(state)

    if not text:
        return {"route": "fallback"}

    # 医学意图优先
    if any(k in text for k in LAB_KEYWORDS):
        return {"route": "lab"}

    if any(k in text for k in HISTORY_KEYWORDS):
        return {"route": "history"}

    if any(k in text for k in SYMPTOM_KEYWORDS):
        return {"route": "symptom"}

    # 寒暄 / 自我介绍 / 元问答
    if _is_greeting_or_meta(text):
        return {"route": "greeting"}

    # 心血管相关表述兜底，避免误拒答（如「心脏跳的有点快」）
    if any(k in text for k in CARDIO_SCOPE_KEYWORDS):
        return {"route": "symptom"}

    return {"route": "fallback"}
