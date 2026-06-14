from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.risk import (
    RISK_SUMMARY_TEMPLATE,
    RISK_LEVEL_EXPLANATIONS,
    RISK_FACTOR_HINTS,
    RISK_LEVEL_ADVICE,
)


def _collect_risk_factors(state: CardiologyState) -> list[str]:
    factors = []

    if state.get("hypertension"):
        factors.append("高血压")
    if state.get("diabetes_mellitus"):
        factors.append("糖尿病")
    if state.get("dyslipidemia"):
        factors.append("血脂异常")
    if state.get("prior_cad"):
        factors.append("冠心病史")
    if state.get("prior_mi"):
        factors.append("心肌梗死史")
    if state.get("smoking_history"):
        factors.append("吸烟")
    if state.get("red_flag_suspected"):
        factors.append("红旗症状或危急检查结果")
    if state.get("investigation_summary"):
        factors.append(f"检查摘要：{state['investigation_summary']}")

    return factors


def _assess_risk_level(state: CardiologyState, factors: list[str]) -> tuple[str, str]:
    if state.get("red_flag_suspected"):
        return "高风险", "red"

    if len(factors) >= 2:
        return "中等偏高风险", "yellow"

    current_triage = state.get("triage_level") or "green"
    if current_triage == "red":
        return "高风险", "red"
    if current_triage == "yellow":
        return "低至中等风险", "yellow"

    return "低至中等风险", "green"


def cardiac_risk_stratification_node(state: CardiologyState) -> dict:
    factors = _collect_risk_factors(state)
    level, triage = _assess_risk_level(state, factors)

    summary = RISK_SUMMARY_TEMPLATE.format(
        risk_level=level,
        risk_factors="、".join(factors) if factors else "当前信息有限",
        risk_explanation=RISK_LEVEL_EXPLANATIONS[level],
    )

    impression = state.get("clinical_impression", "")
    advice = state.get("management_advice", "")
    tier_advice = RISK_LEVEL_ADVICE.get(triage, RISK_LEVEL_ADVICE["green"])

    return {
        "triage_level": triage,
        "clinical_impression": f"{impression}\n\n{summary}".strip(),
        "management_advice": f"{advice}\n\n{RISK_FACTOR_HINTS}\n\n{tier_advice}".strip(),
    }