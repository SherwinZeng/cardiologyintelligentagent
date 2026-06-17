from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.graph.utils import (
    build_standard_llm_fields,
    has_keyword,
    invoke_llm_json,
    latest_user_message,
)
from cardiology_chat.prompts.llm.symptom_llm import SYMPTOM_LLM_SYSTEM
from cardiology_chat.prompts.symptom import (
    RED_FLAG_KEYWORDS,
    RED_FLAG_ADVICE,
    MEDICAL_DISCLAIMER,
    SYMPTOM_FOLLOW_UP,
    SYMPTOM_RESOLVED_KEYWORDS,
    NON_URGENT_SYMPTOM_ADVICE,
)


def _build_symptom_output(
    state: CardiologyState,
    text: str,
    *,
    red_flag: bool,
    llm_data: dict | None = None,
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

    return {
        "chief_complaint": chief_complaint,
        "red_flag_suspected": False,
        "hpi_complete": False,
        **build_standard_llm_fields(
            llm_data,
            triage_fallback="yellow",
            impression_fallback=SYMPTOM_FOLLOW_UP,
            advice_fallback=NON_URGENT_SYMPTOM_ADVICE,
            disclaimer_fallback=MEDICAL_DISCLAIMER,
        ),
    }


def symptom_collection_node(state: CardiologyState) -> dict:
    text = latest_user_message(state)

    # 用户明确说症状缓解 → 走 LLM 正常回复，解除红旗锁
    if has_keyword(text, SYMPTOM_RESOLVED_KEYWORDS, negation_aware=False):
        llm_data = invoke_llm_json(state, SYMPTOM_LLM_SYSTEM)
        output = _build_symptom_output(state, text, red_flag=False, llm_data=llm_data)
        output["red_flag_suspected"] = False
        return output

    # 固定急救模板：仅在本轮新出现红旗词时触发，不用全量历史反复刷屏
    if has_keyword(text, RED_FLAG_KEYWORDS):
        return _build_symptom_output(state, text, red_flag=True)

    llm_data = invoke_llm_json(state, SYMPTOM_LLM_SYSTEM)
    output = _build_symptom_output(state, text, red_flag=False, llm_data=llm_data)
    # 曾触发过红旗且用户仍在症状语境（如「不想去医院」）→ 至少保持 yellow
    if state.get("red_flag_suspected") and output.get("triage_level") == "green":
        output["triage_level"] = "yellow"
    return output
