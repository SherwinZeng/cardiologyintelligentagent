from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.graph.utils import (
    STANDARD_LLM_KEYS,
    all_user_text,
    build_standard_llm_fields,
    has_keyword,
    invoke_llm_json,
    latest_user_message,
    pick_llm_field,
)
from cardiology_chat.prompts.lab import (
    LAB_ACK,
    LAB_FOLLOW_UP,
    LAB_GENERAL_ADVICE,
    URGENT_LAB_ADVICE,
    URGENT_LAB_KEYWORDS,
)
from cardiology_chat.prompts.llm.lab_llm import LAB_LLM_SYSTEM
from cardiology_chat.prompts.symptom import MEDICAL_DISCLAIMER

LAB_LLM_KEYS = STANDARD_LLM_KEYS + ("investigation_summary",)


def _merge_investigation_text(state: CardiologyState, text: str) -> str:
    previous = state.get("investigation_text", "")
    if not previous:
        return text
    if text in previous:
        return previous
    return f"{previous}\n{text}"


def _build_lab_output(
    state: CardiologyState,
    text: str,
    *,
    urgent: bool,
    llm_data: dict | None = None,
) -> dict:
    investigation_text = _merge_investigation_text(state, text)

    if urgent:
        return {
            "investigation_text": investigation_text,
            "investigation_summary": "报告提示可能存在需紧急评估的心血管异常。",
            "red_flag_suspected": True,
            "triage_level": "red",
            "clinical_impression": (
                f"{LAB_ACK}\n\n"
                "您提到的检查结果存在需要警惕的情况，建议尽快到医院急诊或心内科进一步评估。"
            ),
            "management_advice": URGENT_LAB_ADVICE,
            "medical_disclaimer": MEDICAL_DISCLAIMER,
        }

    data = llm_data or {}
    return {
        "investigation_text": investigation_text,
        "investigation_summary": pick_llm_field(
            data, "investigation_summary", "待补充完整报告后进一步解读。"
        ),
        **build_standard_llm_fields(
            llm_data,
            triage_fallback="yellow",
            impression_fallback=f"{LAB_ACK}\n\n{LAB_FOLLOW_UP}",
            advice_fallback=LAB_GENERAL_ADVICE,
            disclaimer_fallback=MEDICAL_DISCLAIMER,
        ),
    }


def lab_report_interpret_node(state: CardiologyState) -> dict:
    text = latest_user_message(state)
    urgent = has_keyword(
        all_user_text(state), URGENT_LAB_KEYWORDS, case_insensitive=True, negation_aware=False
    )

    if urgent:
        return _build_lab_output(state, text, urgent=True)

    llm_data = invoke_llm_json(state, LAB_LLM_SYSTEM, required_keys=LAB_LLM_KEYS)
    return _build_lab_output(state, text, urgent=False, llm_data=llm_data)
