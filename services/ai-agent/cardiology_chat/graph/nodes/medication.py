from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.graph.utils import build_standard_llm_fields, invoke_llm_json
from cardiology_chat.prompts.medication import (
    MEDICATION_ACK,
    MEDICATION_ADVICE,
    MEDICATION_DISCLAIMER,
)
from cardiology_chat.prompts.llm.medication_llm import MEDICATION_LLM_SYSTEM


def medication_consultation_node(state: CardiologyState) -> dict:
    llm_data = invoke_llm_json(state, MEDICATION_LLM_SYSTEM)
    fields = build_standard_llm_fields(
        llm_data,
        triage_fallback="green",
        impression_fallback=MEDICATION_ACK,
        advice_fallback=MEDICATION_ADVICE,
        disclaimer_fallback=MEDICATION_DISCLAIMER,
    )
    return fields
