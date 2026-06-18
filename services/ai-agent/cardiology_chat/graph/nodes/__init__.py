"""业务节点统一导出（供 builder.py 注册）。

分组：
  intake/       信息采集 — 症状、既往史、用药
  diagnostics/  检查流水线 — 报告解读 → 风险评估 → 转诊
  response/     直接回复 — 寒暄、宽口径 fallback

每个节点函数签名： (state: CardiologyState) -> dict
返回的 dict 会 merge 进 state；通常包含 triage_level + 四件套输出字段。
"""

from cardiology_chat.graph.nodes.diagnostics import (
    cardiac_risk_stratification_node,
    lab_report_interpret_node,
    physician_referral_node,
)
from cardiology_chat.graph.nodes.intake import (
    medical_history_inquiry_node,
    medication_consultation_node,
    symptom_collection_node,
)
from cardiology_chat.graph.nodes.response import (
    greeting_response_node,
    medical_fallback_response_node,
)

__all__ = [
    "greeting_response_node",
    "medical_fallback_response_node",
    "symptom_collection_node",
    "medical_history_inquiry_node",
    "medication_consultation_node",
    "lab_report_interpret_node",
    "cardiac_risk_stratification_node",
    "physician_referral_node",
]
