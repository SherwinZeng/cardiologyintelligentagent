from langchain_core.messages import AIMessage, HumanMessage

from cardiology_chat.graph.builder import get_cardiology_graph
from cardiology_chat.graph.checkpointer import build_thread_id, delete_checkpoint_thread


def invoke_general_understanding(uid: str, session: str, message: str) -> dict:
    if not uid or not uid.strip():
        raise ValueError("uid 无效")
    if not session or not session.strip():
        raise ValueError("session 无效")
    if not message or not message.strip():
        raise ValueError("message 无效")

    config = {"configurable": {"thread_id": build_thread_id(uid, session)}}
    graph = get_cardiology_graph()
    result = graph.invoke({"messages": [HumanMessage(content=message.strip())]},config=config,)
    explanation = (result.get("clinical_impression") or "").strip()
    if explanation:
        graph.update_state(config, {"messages": [AIMessage(content=explanation)]})

    return {
        "urgency": result.get("triage_level") or "",
        "explanation": explanation,
        "advice": result.get("management_advice") or "",
        "disclaimer": result.get("medical_disclaimer") or "",
    }


def delete_session_checkpoint(uid: str, session: str) -> None:
    if not uid or not uid.strip() or not session or not session.strip():
        return
    delete_checkpoint_thread(uid.strip(), session.strip())
