from operator import add
from typing import Annotated, TypedDict, Literal
from langchain_core.messages import AnyMessage


class CardiologyState(TypedDict):
    messages: Annotated[list[AnyMessage], add]
    route: Literal["symptom", "history", "lab", "greeting", "fallback"]

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
    investigation_text: str  # 用户提供的检查/化验原文
    investigation_summary: str  # 检查解读摘要

    # ── 分诊与输出 (对应 Java 返回) ──
    triage_level: str  # 分诊级别："" / green / yellow / red
    clinical_impression: str  # 临床印象/解释 → 对应 explanation
    management_advice: str  # 处理建议 → 对应 advice
    medical_disclaimer: str  # 免责声明 → 对应 disclaimer

    # ── 红旗征象 (Red Flags，ACS 等危急信号) ──
    red_flag_suspected: bool  # 是否怀疑高危（持续胸痛、大汗、晕厥等）


def empty_cardiology_state() -> CardiologyState:
    return {
        "messages": [],
        "route": "fallback",
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
        "triage_level": "",
        "clinical_impression": "",
        "management_advice": "",
        "medical_disclaimer": "",
        "red_flag_suspected": False,
    }
