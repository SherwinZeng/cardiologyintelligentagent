"""用药咨询节点（route=medication）。"""

from cardiology_chat.graph.llm.conversation_node import run_standard_conversation_node
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.medication import (
    MEDICATION_ACK,
    MEDICATION_ADVICE,
    MEDICATION_DISCLAIMER,
)
from cardiology_chat.prompts.llm.medication_llm import MEDICATION_LLM_SYSTEM


def medication_consultation_node(state: CardiologyState) -> dict:
    return run_standard_conversation_node(
        state,
        MEDICATION_LLM_SYSTEM,
        route="default",
        triage_fallback="green",
        impression_fallback=MEDICATION_ACK,
        advice_fallback=MEDICATION_ADVICE,
        disclaimer_fallback=MEDICATION_DISCLAIMER,
    )
