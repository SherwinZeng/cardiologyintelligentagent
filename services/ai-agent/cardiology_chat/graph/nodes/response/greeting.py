"""寒暄与元对话节点（route=greeting）。"""

from cardiology_chat.graph.llm.conversation_node import run_standard_conversation_node
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.fallback import MEDICAL_DISCLAIMER_SHORT
from cardiology_chat.prompts.llm.greeting_llm import GREETING_LLM_SYSTEM


def greeting_response_node(state: CardiologyState) -> dict:
    return run_standard_conversation_node(
        state,
        GREETING_LLM_SYSTEM,
        route="greeting",
        triage_fallback="green",
        advice_fallback="如有具体症状或检查报告，可以直接告诉我，我会尽力帮您梳理。",
        disclaimer_fallback=MEDICAL_DISCLAIMER_SHORT,
    )
