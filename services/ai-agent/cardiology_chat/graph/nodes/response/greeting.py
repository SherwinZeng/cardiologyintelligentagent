"""寒暄与元对话节点（route=greeting）。"""

from cardiology_chat.graph.llm import latest_user_message
from cardiology_chat.graph.llm.conversation_node import run_standard_conversation_node
from cardiology_chat.graph.memory import is_user_name_recall
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.fallback import MEDICAL_DISCLAIMER_SHORT
from cardiology_chat.prompts.llm.greeting_llm import GREETING_LLM_SYSTEM


def _user_name_recall_output(name: str) -> dict:
    return {
        "triage_level": "green",
        "clinical_impression": (
            f"当然记得呀，您刚才告诉我您叫{name}。"
            "我会在这段对话里尽量记住这个称呼，后面也可以这样叫您 🌸"
        ),
        "management_advice": (
            "如果您愿意，可以继续告诉我想聊的心血管健康问题，比如胸闷胸痛、"
            "心慌、血压、用药或检查报告，我会帮您一起梳理。"
        ),
        "medical_disclaimer": MEDICAL_DISCLAIMER_SHORT,
    }


def greeting_response_node(state: CardiologyState) -> dict:
    user_text = latest_user_message(state)
    if is_user_name_recall(user_text):
        user_name = state.get("user_display_name", "")
        if user_name:
            return _user_name_recall_output(user_name)

    return run_standard_conversation_node(
        state,
        GREETING_LLM_SYSTEM,
        route="greeting",
        triage_fallback="green",
        advice_fallback="如有具体症状或检查报告，可以直接告诉我，我会尽力帮您梳理。",
        disclaimer_fallback=MEDICAL_DISCLAIMER_SHORT,
    )
