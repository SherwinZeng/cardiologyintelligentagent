"""指南 PDF 文件名 → 中文展示名。"""

from __future__ import annotations

import re

# key 为 PDF stem（不含 .pdf），与 Milvus metadata.guide_name 一致
GUIDE_DISPLAY_NAMES: dict[str, str] = {
    "NationalGuidelinesforPrimaryHypertensionPreventionandManagement2025Edition": (
        "国家基层高血压防治管理指南（2025版）"
    ),
    "ChineseGuidelinesfortheDiagnosisandTreatmentofHeartFailure2024": (
        "中国心力衰竭诊断和治疗指南（2024）"
    ),
    "ChineseGuidelinesfortheManagementofAtrialFibrillation(2025)": (
        "中国心房颤动管理指南（2025）"
    ),
    "GuidelinesfortheDiagnosisandTreatmentofAcuteST-ElevationMyocardialInfarction(2019)": (
        "急性ST段抬高型心肌梗死诊断和治疗指南（2019）"
    ),
    "InterpretationoftheGuidelinesforDiagnosisandTreatmentofNon-ST-ElevationAcuteCoronarySyndrome(2024)": (
        "非ST段抬高型急性冠脉综合征指南解读（2024）"
    ),
    "ChineseGuidelinesforLipidManagement(CommunityVersion2024)": (
        "中国血脂管理指南（基层版2024）"
    ),
    "ChineseAdultEchocardiographyMeasurementGuidelines": (
        "中国成年人超声心动图测量指南"
    ),
    "ExpertConsensuson18-Lead HolterMonitoring": (
        "18导联动态心电图监测专家共识"
    ),
    "ExpertConsensusonStandardizedWritingofElectrocardiogramReports(2026)": (
        "心电图报告规范化书写专家共识（2026）"
    ),
    "ExpertConsensusonIntra-coronaryThrombolysisDuringPercutaneousCoronaryInterventionforAcuteST-ElevationMyocardialInfarction(2025)": (
        "急性ST段抬高型心肌梗死PCI冠脉内溶栓专家共识（2025）"
    ),
    "Chinese Expert Consensus on the Treatment of Cardiogenic Stroke (2022)": (
        "心源性卒中治疗中国专家共识（2022）"
    ),
}

_EXCERPT_GUIDE_RE = re.compile(r"^\[([^\]]+)\]")


def normalize_guide_stem(stem: str) -> str:
    return (stem or "").strip()


def resolve_guide_display_name(stem: str) -> str:
    """将 guide_name stem 转为中文名；未知则返回原 stem。"""
    key = normalize_guide_stem(stem)
    if not key:
        return ""
    if key in GUIDE_DISPLAY_NAMES:
        return GUIDE_DISPLAY_NAMES[key]
    # 兼容 ingest 时 stem 轻微差异
    compact = re.sub(r"\s+", "", key)
    for raw, title in GUIDE_DISPLAY_NAMES.items():
        if re.sub(r"\s+", "", raw) == compact:
            return title
    return key


def extract_guide_stem_from_excerpt(excerpt: str) -> str:
    match = _EXCERPT_GUIDE_RE.match((excerpt or "").strip())
    return match.group(1) if match else ""


def unique_guide_display_names(stems: list[str]) -> list[str]:
    seen: set[str] = set()
    names: list[str] = []
    for stem in stems:
        display = resolve_guide_display_name(stem)
        if not display or display in seen:
            continue
        seen.add(display)
        names.append(display)
    return names
