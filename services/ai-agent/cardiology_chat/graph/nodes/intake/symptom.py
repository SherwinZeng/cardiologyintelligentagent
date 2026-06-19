"""症状采集节点（route=symptom）。

红旗词 → 固定 red；其余走统一多轮 LLM + 静态兜底。
"""

from cardiology_chat.graph.llm import all_user_text, has_keyword, latest_user_message
from cardiology_chat.graph.llm.conversation_node import run_standard_conversation_node
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.fallback import (
    ER_DOUBT_RED_FALLBACK,
    ER_DOUBT_RED_ADVICE,
    is_er_doubt_question,
    resolve_symptom_static_impression,
)
from cardiology_chat.prompts.llm.symptom_llm import SYMPTOM_LLM_SYSTEM
from cardiology_chat.prompts.symptom import (
    RED_FLAG_KEYWORDS,
    RED_FLAG_ADVICE,
    MEDICAL_DISCLAIMER,
    SYMPTOM_PARTIAL_RELIEF_MARKERS,
    SYMPTOM_RESOLVED_KEYWORDS,
    NON_URGENT_SYMPTOM_ADVICE,
)

_SUDDEN_ONSET_MARKERS = ("突然", "突发", "突然间", "一下子", "忽然")
_PRESSURE_PAIN_MARKERS = ("压榨", "压迫", "胸口被压", "胸口压")

# 高危上下文必须跨轮识别，不能只看当前一句。
def _is_high_risk_context(state: CardiologyState) -> bool:
    if state.get("red_flag_suspected") or state.get("triage_level") == "red":
        return True
    text = all_user_text(state)
    if has_keyword(text, RED_FLAG_KEYWORDS, negation_aware=False):
        return True
    return has_keyword(text, _SUDDEN_ONSET_MARKERS, negation_aware=False) and has_keyword(
        text,
        _PRESSURE_PAIN_MARKERS,
        negation_aware=False,
    )


def _is_symptom_fully_resolved(state: CardiologyState, text: str) -> bool:
    """部分缓解（如「现在好多了」）或高危语境下，不能当作症状已结束。"""
    if not has_keyword(text, SYMPTOM_RESOLVED_KEYWORDS, negation_aware=False):
        return False
    if has_keyword(text, SYMPTOM_PARTIAL_RELIEF_MARKERS, negation_aware=False):
        return False
    if _is_high_risk_context(state):
        return False
    return True

# LLM 失败时的静态兜底，随高危上下文升级。
def _symptom_fallbacks(state: CardiologyState, text: str) -> tuple[str, str, str]:
    impression = resolve_symptom_static_impression(state, text)
    if _is_high_risk_context(state):
        return "red", impression, RED_FLAG_ADVICE
    return "yellow", impression, NON_URGENT_SYMPTOM_ADVICE

# 高危场景下用户质疑是否就医：确定性快路径，不让 LLM 降级成问卷。
def _build_high_risk_er_doubt_output(state: CardiologyState, text: str) -> dict:
    return {
        "chief_complaint": state.get("chief_complaint") or text,
        "red_flag_suspected": True,
        "hpi_complete": False,
        "triage_level": "red",
        "clinical_impression": ER_DOUBT_RED_FALLBACK,
        "management_advice": ER_DOUBT_RED_ADVICE,
        "medical_disclaimer": MEDICAL_DISCLAIMER,
    }


# 标准化 symptom 节点输出，统一写四件套和红旗状态。
def _build_symptom_output(
    state: CardiologyState,
    text: str,
    *,
    red_flag: bool,
    llm_fields: dict | None = None,
) -> dict:
    chief_complaint = state.get("chief_complaint") or text

    if red_flag:
        return {
            "chief_complaint": chief_complaint,
            "red_flag_suspected": True,
            "hpi_complete": False,
            "triage_level": "red",
            "clinical_impression": (
                "根据您当前描述，需警惕急性心血管事件或其他危重情况的可能，"
                "建议立即就医评估。"
            ),
            "management_advice": RED_FLAG_ADVICE,
            "medical_disclaimer": MEDICAL_DISCLAIMER,
        }

    output = {
        "chief_complaint": chief_complaint,
        "red_flag_suspected": _is_high_risk_context(state),
        "hpi_complete": False,
        **(llm_fields or {}),
    }
    if _is_high_risk_context(state):
        if output.get("triage_level") not in ("red",):
            output["triage_level"] = "red"
        output["red_flag_suspected"] = True
    elif state.get("red_flag_suspected") and output.get("triage_level") == "green":
        output["triage_level"] = "yellow"
    return output


def symptom_collection_node(state: CardiologyState) -> dict:
    text = latest_user_message(state)
    triage_fallback, impression_fallback, advice_fallback = _symptom_fallbacks(state, text)
    if _is_high_risk_context(state) and is_er_doubt_question(text):
        return _build_high_risk_er_doubt_output(state, text)

    if _is_symptom_fully_resolved(state, text):
        llm_fields = run_standard_conversation_node(
            state,
            SYMPTOM_LLM_SYSTEM,
            route="symptom",
            triage_fallback=triage_fallback,
            advice_fallback=advice_fallback,
            disclaimer_fallback=MEDICAL_DISCLAIMER,
            impression_fallback=impression_fallback,
        )
        output = _build_symptom_output(state, text, red_flag=False, llm_fields=llm_fields)
        output["red_flag_suspected"] = False
        return output

    if has_keyword(text, RED_FLAG_KEYWORDS):
        return _build_symptom_output(state, text, red_flag=True)

    llm_fields = run_standard_conversation_node(
        state,
        SYMPTOM_LLM_SYSTEM,
        route="symptom",
        triage_fallback=triage_fallback,
        advice_fallback=advice_fallback,
        disclaimer_fallback=MEDICAL_DISCLAIMER,
        impression_fallback=impression_fallback,
    )
    return _build_symptom_output(state, text, red_flag=False, llm_fields=llm_fields)
