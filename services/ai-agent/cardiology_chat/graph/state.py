"""LangGraph 共享状态 CardiologyState。

所有节点读写的字段定义在这里。节点函数返回 dict，LangGraph 会 merge 进 state。

字段分区：
  - user_display_name  用户称呼（checkpointer 持久化；greeting 写入）
  - hpi_* / pmh_*        采集中间结果（现病史 / 既往史）
  - investigation_*      检查解读上下文
  - triage_level 等四件套  最终输出，映射到 Java GeneralUnderstandingResponse：
      triage_level        → urgency
      clinical_impression → explanation
      management_advice   → advice
      medical_disclaimer  → disclaimer
"""

from operator import add
from typing import Annotated, Literal, TypedDict

from langchain_core.messages import AnyMessage


class CardiologyState(TypedDict):
    # messages 使用 Annotated[..., add]：新消息 append 而非覆盖
    messages: Annotated[list[AnyMessage], add]
    route: Literal["symptom", "history", "lab", "medication", "greeting", "fallback"]

    # ── 对话事实记忆 ──
    # structured_memory 是正式记忆；conversation_memory 是给旧节点/兜底逻辑用的扁平缓存。
    structured_memory: dict[str, object]
    conversation_memory: dict[str, object]  # checkpointer 跨轮恢复的稳定事实，如用户称呼
    user_display_name: str  # 用户自我介绍的称呼

    # ── 对话策略与上下文包 ──
    dialogue_policy: str  # identity_recall / emergency_red / symptom_intake / ...
    context_bundle: dict[str, object]  # 给 LLM 的结构化上下文，而不是只塞最近 12 条消息

    # ── 采集完成度 ──
    hpi_complete: bool  # 现病史是否采集完整
    pmh_complete: bool  # 既往史是否采集完整

    # ── 主诉 & 现病史 (HPI: History of Present Illness) ──
    chief_complaint: str  # 主诉：患者最主要的不适
    symptom_onset: str  # 起病时间：何时开始
    symptom_duration: str  # 持续时间
    symptom_character: str  # 症状性质：压榨痛、刺痛、闷痛等
    symptom_radiation: str  # 放射痛：向左肩/后背 等
    aggravating_factors: str  # 加重因素：活动、情绪、饱餐等
    relieving_factors: str  # 缓解因素：休息、硝酸甘油等
    associated_symptoms: str  # 伴随症状：气短、大汗、恶心、晕厥等

    # ── 既往史 (PMH: Past Medical History) ──
    hypertension: bool  # 高血压病史
    diabetes_mellitus: bool  # 糖尿病病史
    dyslipidemia: bool  # 血脂异常
    prior_cad: bool  # 既往冠心病史 (CAD)
    prior_mi: bool  # 既往心肌梗死 (MI)
    smoking_history: bool  # 吸烟史
    family_premature_cad: str  # 家族早发冠心病史

    # ── 检查/化验 ──
    investigation_text: str  # 用户提供的检查/化验原文（多轮累积）
    investigation_summary: str  # 检查解读摘要
    lab_followup_needed: bool  # 是否继续走风险分层 + 转诊（有具体报告/异常时为 True）

    # ── 分诊与输出 (对应 Java 返回) ──
    triage_level: str  # 分诊级别："" / green / yellow / red
    clinical_impression: str  # 临床印象/解释 → 对应 explanation
    management_advice: str  # 处理建议 → 对应 advice
    medical_disclaimer: str  # 免责声明 → 对应 disclaimer

    # ── 红旗征象 (Red Flags，ACS 等危急信号) ──
    red_flag_suspected: bool  # 是否怀疑高危（持续胸痛、大汗、晕厥等）


def empty_cardiology_state() -> CardiologyState:
    """新 session 的初始 state。"""
    return {
        "messages": [],
        "route": "fallback",
        "structured_memory": {},
        "conversation_memory": {},
        "user_display_name": "",
        "dialogue_policy": "",
        "context_bundle": {},
        "hpi_complete": False,
        "pmh_complete": False,
        "chief_complaint": "",
        "symptom_onset": "",
        "symptom_duration": "",
        "symptom_character": "",
        "symptom_radiation": "",
        "aggravating_factors": "",
        "relieving_factors": "",
        "associated_symptoms": "",
        "hypertension": False,
        "diabetes_mellitus": False,
        "dyslipidemia": False,
        "prior_cad": False,
        "prior_mi": False,
        "smoking_history": False,
        "family_premature_cad": "",
        "investigation_text": "",
        "investigation_summary": "",
        "lab_followup_needed": False,
        "triage_level": "",
        "clinical_impression": "",
        "management_advice": "",
        "medical_disclaimer": "",
        "red_flag_suspected": False,
    }
