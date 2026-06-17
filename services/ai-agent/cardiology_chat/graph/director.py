from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import StateGraph, START, END

from cardiology_chat.graph.nodes import (
    clinical_dispatch_node,
    route_after_dispatch,
    medical_fallback_response_node,
    greeting_response_node,
    symptom_collection_node,
    medical_history_inquiry_node,
    lab_report_interpret_node,
    cardiac_risk_stratification_node,
    physician_referral_node,
    medication_consultation_node,
)
from cardiology_chat.graph.state import CardiologyState

builder = StateGraph(CardiologyState)

# ── 节点注册（顺序无关，仅声明图中有哪些节点）──
builder.add_node("clinical_dispatch_node", clinical_dispatch_node)
builder.add_node("symptom_collection_node", symptom_collection_node)
builder.add_node("medical_history_inquiry_node", medical_history_inquiry_node)
builder.add_node("lab_report_interpret_node", lab_report_interpret_node)
builder.add_node("cardiac_risk_stratification_node", cardiac_risk_stratification_node)
builder.add_node("physician_referral_node", physician_referral_node)
builder.add_node("greeting_response_node", greeting_response_node)
builder.add_node("medication_consultation_node", medication_consultation_node)
builder.add_node("medical_fallback_response_node", medical_fallback_response_node)

# ── 边 = 运行时路径（见 README 流程图；非线性流水线）──
builder.add_edge(START, "clinical_dispatch_node")
builder.add_conditional_edges(
    "clinical_dispatch_node",
    route_after_dispatch,
    {
        "symptom": "symptom_collection_node",
        "history": "medical_history_inquiry_node",
        "medication": "medication_consultation_node",
        "lab": "lab_report_interpret_node",
        "greeting": "greeting_response_node",
        "fallback": "medical_fallback_response_node",
    },
)

builder.add_edge("symptom_collection_node", END)
builder.add_edge("medical_history_inquiry_node", END)
builder.add_edge("medication_consultation_node", END)
builder.add_edge("greeting_response_node", END)
builder.add_edge("lab_report_interpret_node", "cardiac_risk_stratification_node")
builder.add_edge("cardiac_risk_stratification_node", "physician_referral_node")
builder.add_edge("physician_referral_node", END)
builder.add_edge("medical_fallback_response_node", END)

cardiology_graph = builder.compile(checkpointer=InMemorySaver())
