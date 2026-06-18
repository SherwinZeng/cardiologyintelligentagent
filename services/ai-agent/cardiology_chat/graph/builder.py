"""LangGraph 图编排（原 director.py）。

每轮用户消息的流转路径：

  START
    → conversation_memory_node（从 Java history 恢复结构化对话事实）
    → clinical_dispatch_node（routing/dispatch.py：LLM 意图路由）
    → 按 route 字段进入 ONE OF：
        symptom   → symptom_collection_node      → END
        history   → medical_history_inquiry_node → END
        medication→ medication_consultation_node → END
        greeting  → greeting_response_node        → END
        fallback  → medical_fallback_response_node→ END
        lab       → lab_report_interpret_node
                    →（有具体报告时）cardiac_risk_stratification_node
                    → physician_referral_node → END
                    →（科普/怎么看）直接 END

说明：
  - lab 路径：有具体报告/异常时串联 3 节点；科普「怎么看」类单节点 END
  - 其余 route 单节点直接 END
  - 跨轮上下文由 Java 传入 history，图本身无 checkpointer
"""

from langgraph.graph import END, START, StateGraph

from cardiology_chat.graph.memory import conversation_memory_node
from cardiology_chat.graph.nodes import (
    cardiac_risk_stratification_node,
    greeting_response_node,
    lab_report_interpret_node,
    medical_fallback_response_node,
    medical_history_inquiry_node,
    medication_consultation_node,
    physician_referral_node,
    symptom_collection_node,
)
from cardiology_chat.graph.routing import clinical_dispatch_node, route_after_dispatch
from cardiology_chat.graph.state import CardiologyState


def route_after_lab(state: CardiologyState) -> str:
    """有具体报告/异常才继续风险分层；科普类 lab 单节点结束。"""
    if state.get("lab_followup_needed"):
        return "cardiac_risk_stratification_node"
    return END


builder = StateGraph(CardiologyState)

# ── 注册所有节点（名称即 builder 内部 ID，与 conditional_edges 映射一致）──
builder.add_node("conversation_memory_node", conversation_memory_node)
builder.add_node("clinical_dispatch_node", clinical_dispatch_node)
builder.add_node("symptom_collection_node", symptom_collection_node)
builder.add_node("medical_history_inquiry_node", medical_history_inquiry_node)
builder.add_node("lab_report_interpret_node", lab_report_interpret_node)
builder.add_node("cardiac_risk_stratification_node", cardiac_risk_stratification_node)
builder.add_node("physician_referral_node", physician_referral_node)
builder.add_node("greeting_response_node", greeting_response_node)
builder.add_node("medication_consultation_node", medication_consultation_node)
builder.add_node("medical_fallback_response_node", medical_fallback_response_node)

# ── 边：入口先恢复对话事实，再走 dispatch，由 state["route"] 分支 ──
builder.add_edge(START, "conversation_memory_node")
builder.add_edge("conversation_memory_node", "clinical_dispatch_node")
builder.add_conditional_edges(
    "clinical_dispatch_node",
    route_after_dispatch,  # 读取 state["route"] 返回分支名
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
# lab：有具体报告才走风险 + 转诊；科普类单节点结束
builder.add_conditional_edges(
    "lab_report_interpret_node",
    route_after_lab,
    {
        "cardiac_risk_stratification_node": "cardiac_risk_stratification_node",
        END: END,
    },
)
builder.add_edge("cardiac_risk_stratification_node", "physician_referral_node")
builder.add_edge("physician_referral_node", END)
builder.add_edge("medical_fallback_response_node", END)

cardiology_graph = builder.compile()
