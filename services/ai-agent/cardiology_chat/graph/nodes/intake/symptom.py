"""症状采集节点（route=symptom）。

红旗词 → 固定 red；其余走统一多轮 LLM + 静态兜底。
"""

from cardiology_chat.graph.llm import has_keyword, latest_user_message
from cardiology_chat.graph.llm.conversation_node import run_standard_conversation_node
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.llm.symptom_llm import SYMPTOM_LLM_SYSTEM
from cardiology_chat.prompts.symptom import (
    RED_FLAG_KEYWORDS,
    RED_FLAG_ADVICE,
    MEDICAL_DISCLAIMER,
    SYMPTOM_RESOLVED_KEYWORDS,
    NON_URGENT_SYMPTOM_ADVICE,
)


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
        "red_flag_suspected": False,
        "hpi_complete": False,
        **(llm_fields or {}),
    }
    if state.get("red_flag_suspected") and output.get("triage_level") == "green":
        output["triage_level"] = "yellow"
    return output


def symptom_collection_node(state: CardiologyState) -> dict:
    text = latest_user_message(state)

    if has_keyword(text, SYMPTOM_RESOLVED_KEYWORDS, negation_aware=False):
        llm_fields = run_standard_conversation_node(
            state,
            SYMPTOM_LLM_SYSTEM,
            route="symptom",
            triage_fallback="yellow",
            advice_fallback=NON_URGENT_SYMPTOM_ADVICE,
            disclaimer_fallback=MEDICAL_DISCLAIMER,
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
        triage_fallback="yellow",
        advice_fallback=NON_URGENT_SYMPTOM_ADVICE,
        disclaimer_fallback=MEDICAL_DISCLAIMER,
    )
    return _build_symptom_output(state, text, red_flag=False, llm_fields=llm_fields)
