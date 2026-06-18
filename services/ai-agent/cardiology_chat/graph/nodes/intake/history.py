"""既往史采集节点（route=history）。

除 LLM 回复外，还会从对话文本里规则提取 PMH 布尔标记（高血压、糖尿病等），
写入 state 供后续 risk 节点使用。
"""

from cardiology_chat.graph.llm import (
    all_user_text,
    invoke_llm_json_with_retry,
    latest_user_message,
)
from cardiology_chat.graph.llm.invoke import build_standard_llm_fields
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.history import (
    HISTORY_FOLLOW_UP,
    HISTORY_ACK,
    HISTORY_ADVICE,
    MEDICAL_DISCLAIMER,
    HIGH_RISK_COMBOS,
    HIGH_RISK_PMH_ADVICE,
)
from cardiology_chat.prompts.llm.history_llm import HISTORY_LLM_SYSTEM


def _extract_pmh_flags(text: str) -> dict:
    return {
        "hypertension": any(k in text for k in ("高血压", "血压高")),
        "diabetes_mellitus": any(k in text for k in ("糖尿病", "血糖高")),
        "dyslipidemia": any(k in text for k in ("血脂", "胆固醇", "甘油三酯")),
        "prior_cad": any(k in text for k in ("冠心病", "冠脉", "心绞痛")),
        "prior_mi": any(k in text for k in ("心梗", "心肌梗死", "心肌梗塞")),
        "smoking_history": any(k in text for k in ("吸烟", "抽烟")),
    }


def _merge_pmh_flags(state: CardiologyState, text: str) -> dict:
    current = _extract_pmh_flags(text)
    return {
        "hypertension": state.get("hypertension") or current["hypertension"],
        "diabetes_mellitus": state.get("diabetes_mellitus") or current["diabetes_mellitus"],
        "dyslipidemia": state.get("dyslipidemia") or current["dyslipidemia"],
        "prior_cad": state.get("prior_cad") or current["prior_cad"],
        "prior_mi": state.get("prior_mi") or current["prior_mi"],
        "smoking_history": state.get("smoking_history") or current["smoking_history"],
    }


def _is_high_risk_pmh(text: str) -> bool:
    return any(all(k in text for k in combo) for combo in HIGH_RISK_COMBOS)


def _build_history_output(
    text: str,
    state: CardiologyState,
    *,
    pmh: dict,
    high_risk: bool,
    llm_data: dict | None = None,
) -> dict:
    fields = build_standard_llm_fields(
        llm_data,
        triage_fallback="green",
        impression_fallback=f"{HISTORY_ACK}\n\n{HISTORY_FOLLOW_UP}",
        advice_fallback=HISTORY_ADVICE,
        disclaimer_fallback=MEDICAL_DISCLAIMER,
    )
    if high_risk:
        fields["triage_level"] = "yellow"
        fields["management_advice"] = (
            f"{HIGH_RISK_PMH_ADVICE}\n\n{fields['management_advice']}".strip()
        )

    family_history = state.get("family_premature_cad", "")
    if "家族" in text:
        family_history = all_user_text(state)

    return {
        **pmh,
        "family_premature_cad": family_history,
        "pmh_complete": False,
        **fields,
    }


def medical_history_inquiry_node(state: CardiologyState) -> dict:
    """既往史采集：规则提取 PMH 布尔标记 + LLM 生成追问与回复。

    高风险 PMH 组合会上调 triage 至 yellow，供后续 risk 节点使用。
    """
    text = latest_user_message(state)
    conversation_text = all_user_text(state)
    pmh = _merge_pmh_flags(state, conversation_text)
    high_risk = _is_high_risk_pmh(conversation_text)
    llm_data = invoke_llm_json_with_retry(state, HISTORY_LLM_SYSTEM, use_conversation_rules=True)

    return _build_history_output(
        text,
        state,
        pmh=pmh,
        high_risk=high_risk,
        llm_data=llm_data,
    )
