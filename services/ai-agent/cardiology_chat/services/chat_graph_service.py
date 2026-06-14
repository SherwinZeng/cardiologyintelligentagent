from langchain_core.messages import HumanMessage

from cardiology_chat.graph import cardiology_graph


def verify_uid(uid: str) -> None:
    """uid 仅用于身份校验，不参与 LangGraph 记忆（thread_id）。"""
    if not uid or not uid.strip():
        raise ValueError("uid 无效")


def invoke_general_understanding(
        uid: str,
        session: str,
        message: str,
) -> dict:
    verify_uid(uid)

    thread_id = session.strip()
    result = cardiology_graph.invoke(
        {"messages": [HumanMessage(content=message)]},
        config={"configurable": {"thread_id": thread_id}},
    )
    return {
        "urgency": result.get("triage_level") or "",
        "explanation": result.get("clinical_impression") or "",
        "advice": result.get("management_advice") or "",
        "disclaimer": result.get("medical_disclaimer") or "",
    }
