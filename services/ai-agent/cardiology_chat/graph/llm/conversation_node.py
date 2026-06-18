"""多轮对话节点的统一入口：LLM 重试 + 静态 impression 兜底。"""

from cardiology_chat.graph.llm.invoke import (
    STANDARD_LLM_KEYS,
    build_standard_llm_fields,
    invoke_llm_json_with_retry,
)
from cardiology_chat.graph.llm.messages import latest_user_message
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.fallback import RouteKind, resolve_static_impression


def run_standard_conversation_node(
    state: CardiologyState,
    system_prompt: str,
    *,
    route: RouteKind,
    triage_fallback: str,
    advice_fallback: str,
    disclaimer_fallback: str,
    required_keys: tuple[str, ...] = STANDARD_LLM_KEYS,
    impression_fallback: str | None = None,
) -> dict:
    """多轮 LLM 节点标准流程：shared 对话规则 + 重试 + 静态兜底。"""
    user_text = latest_user_message(state)
    llm_data = invoke_llm_json_with_retry(
        state,
        system_prompt,
        required_keys=required_keys,
        use_conversation_rules=True,
    )
    static_impression = impression_fallback or resolve_static_impression(user_text, route)
    return build_standard_llm_fields(
        llm_data,
        triage_fallback=triage_fallback,
        impression_fallback=static_impression,
        advice_fallback=advice_fallback,
        disclaimer_fallback=disclaimer_fallback,
    )
