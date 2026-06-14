from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.graph.utils import (
    all_user_text,
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
    red_flag = has_keyword(all_user_text(state), RED_FLAG_KEYWORDS)

    if red_flag:
        return _build_symptom_output(state, text, red_flag=True)

    llm_data = invoke_llm_json(state, SYMPTOM_LLM_SYSTEM)
    return _build_symptom_output(state, text, red_flag=False, llm_data=llm_data)
