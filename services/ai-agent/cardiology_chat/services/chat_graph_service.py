from langchain_core.messages import HumanMessage, AIMessage

from cardiology_chat.graph import cardiology_graph
from cardiology_chat.graph.state import empty_cardiology_state


def invoke_general_understanding(uid: str, session: str, message: str, history: list[dict] | None = None) -> dict:
    if not uid or not uid.strip():
        raise ValueError("uid 无效")
    messages = []
    for messages_chunk in history or []:
        role = messages_chunk.get("role") if isinstance(messages_chunk, dict) else getattr(messages_chunk, "role", None)
        content = messages_chunk.get("content", "") if isinstance(messages_chunk, dict) else getattr(messages_chunk, "content", "")
        if role == "user":
            messages.append(HumanMessage(content=content))
        elif role == "assistant":
            messages.append(AIMessage(content=content))
    messages.append(HumanMessage(content=message))
    state = empty_cardiology_state()
    state["messages"] = messages
    result = cardiology_graph.invoke(state)
    return {
        "urgency": result.get("triage_level") or "",
        "explanation": result.get("clinical_impression") or "",
        "advice": result.get("management_advice") or "",
        "disclaimer": result.get("medical_disclaimer") or "",
    }
