"""宽口径兜底节点（route=fallback）。"""

from cardiology_chat.graph.llm import invoke_llm_json_with_retry, latest_user_message
from cardiology_chat.graph.llm.invoke import build_standard_llm_fields
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.fallback import (
    GENERIC_RECEIVED_FALLBACK,
    MEDICAL_DISCLAIMER_SHORT,
    NON_CARDIO_FALLBACK,
    resolve_static_impression,
)
from cardiology_chat.prompts.llm.fallback_llm import FALLBACK_LLM_SYSTEM

FALLBACK_LLM_KEYS = ("urgency", "explanation", "advice", "disclaimer")


def medical_fallback_response_node(state: CardiologyState) -> dict:
    user_text = latest_user_message(state)
    llm_data = invoke_llm_json_with_retry(
        state,
        FALLBACK_LLM_SYSTEM,
        required_keys=FALLBACK_LLM_KEYS,
        use_conversation_rules=True,
    )

    if not llm_data:
        impression_fallback = resolve_static_impression(user_text, "fallback")
    elif llm_data.get("is_off_topic") is True:
        impression_fallback = NON_CARDIO_FALLBACK
    else:
        impression_fallback = GENERIC_RECEIVED_FALLBACK

    return build_standard_llm_fields(
        llm_data,
        triage_fallback="green",
        impression_fallback=impression_fallback,
        advice_fallback="欢迎继续提问心血管相关问题，我会温柔陪伴您 💚",
        disclaimer_fallback=MEDICAL_DISCLAIMER_SHORT,
    )
