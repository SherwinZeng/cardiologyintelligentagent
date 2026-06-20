"""向量初筛 + 关键词/主题重排，提高指南检索精度。"""

from __future__ import annotations

import re
from dataclasses import dataclass

from langchain_core.documents import Document

# 查询意图 → 应优先匹配的指南用语
TOPIC_TERMS: dict[str, tuple[str, ...]] = {
    "diet": (
        "饮食",
        "膳食",
        "限盐",
        "低钠",
        "钠盐",
        "DASH",
        "食物",
        "摄入",
        "营养",
        "限酒",
        "酒精",
        "肥胖",
        "体重管理",
        "生活方式干预",
    ),
    "lifestyle": (
        "生活方式",
        "运动",
        "锻炼",
        "久坐",
        "戒烟",
        "限酒",
        "睡眠",
        "心理",
        "康复",
        "自我管理",
    ),
    "medication": (
        "药物",
        "用药",
        "剂量",
        "降压",
        "利尿",
        "ACEI",
        "ARB",
        "CCB",
        "β受体",
        "他汀",
        "抗凝",
        "抗血小板",
        "起始",
        "联合",
    ),
    "lab": (
        "检查",
        "检验",
        "监测",
        "血压",
        "血脂",
        "血糖",
        "肌酐",
        "心电图",
        "超声",
        "动态血压",
        "实验室",
    ),
    "symptom": (
        "症状",
        "胸痛",
        "胸闷",
        "气短",
        "呼吸困难",
        "水肿",
        "晕厥",
        "心悸",
        "乏力",
        "夜间阵发性",
    ),
    "diagnosis": (
        "诊断",
        "定义",
        "分类",
        "分期",
        "分级",
        "标准",
        "筛查",
    ),
}

TOPIC_TRIGGERS: dict[str, tuple[str, ...]] = {
    "diet": ("饮食", "吃什么", "膳食", "限盐", "钠", "营养", "胖", "体重", "盐", "喝酒", "戒酒"),
    "lifestyle": ("运动", "锻炼", "活动", "平时注意", "生活", "日常", "保养", "预防", "注意什么"),
    "medication": ("药", "用药", "吃什么药", "降压", "剂量", "副作用", "联合用药"),
    "lab": ("检查", "化验", "检验", "监测", "指标", "报告"),
    "symptom": ("症状", "疼", "痛", "闷", "喘", "晕", "肿", "心悸", "不舒服"),
    "diagnosis": ("诊断", "是不是", "算不算", "标准", "定义"),
}

# 用户问具体主题时，这些块通常是噪声
NOISE_WHEN_SPECIFIC: dict[str, tuple[str, ...]] = {
    "diet": ("评估内容", "病史：", "合并症包括", "设备应配备", "表 4 基层常用降压药物"),
    "lifestyle": ("评估内容", "合并症包括", "表 4 基层常用降压药物"),
    "medication": ("评估内容", "设备应配备", "体质类型"),
    "lab": ("体质类型", "饮食调理"),
}

_QUERY_NGRAM = re.compile(r"[\u4e00-\u9fff]{2,4}")


@dataclass(frozen=True)
class ScoredDoc:
    document: Document
    score: float


def detect_query_topics(query: str) -> list[str]:
    q = (query or "").strip()
    if not q:
        return []
    topics: list[str] = []
    for topic, triggers in TOPIC_TRIGGERS.items():
        if any(trigger in q for trigger in triggers):
            topics.append(topic)
    return topics


def _query_ngrams(query: str) -> set[str]:
    return set(_QUERY_NGRAM.findall(query))


def _topic_keyword_score(query: str, content: str, topics: list[str]) -> float:
    score = 0.0
    for topic in topics:
        for term in TOPIC_TERMS.get(topic, ()):
            in_query = term in query
            in_content = term in content
            if in_query and in_content:
                score += 4.0
            elif in_content:
                score += 1.5
        for noise in NOISE_WHEN_SPECIFIC.get(topic, ()):
            if noise in content:
                score -= 2.0
    return score


def _direct_overlap_score(query: str, content: str) -> float:
    score = 0.0
    for term in _query_ngrams(query):
        if term in content:
            score += min(len(term) * 0.6, 2.4)
    return score


def score_document(
    query: str,
    content: str,
    *,
    vector_distance: float,
    vector_rank: int,
    topics: list[str],
    vector_weight: float,
    keyword_weight: float,
) -> float:
    # Milvus 默认 L2：距离越小越相似
    vector_score = 1.0 / (1.0 + max(vector_distance, 0.0)) + 0.05 / (vector_rank + 1)

    keyword_score = 0.0
    if topics:
        keyword_score += _topic_keyword_score(query, content, topics)
    keyword_score += _direct_overlap_score(query, content)

    return vector_weight * vector_score + keyword_weight * keyword_score


def rerank_scored_documents(
    query: str,
    docs_with_scores: list[tuple[Document, float]],
    *,
    final_k: int,
    vector_weight: float = 0.35,
    keyword_weight: float = 0.65,
) -> list[Document]:
    if not docs_with_scores:
        return []
    topics = detect_query_topics(query)
    ranked: list[ScoredDoc] = []
    for rank, (doc, distance) in enumerate(docs_with_scores):
        content = (doc.page_content or "").strip()
        if not content:
            continue
        combined = score_document(
            query,
            content,
            vector_distance=distance,
            vector_rank=rank,
            topics=topics,
            vector_weight=vector_weight,
            keyword_weight=keyword_weight,
        )
        ranked.append(ScoredDoc(document=doc, score=combined))

    ranked.sort(key=lambda item: item.score, reverse=True)
    return [item.document for item in ranked[:final_k]]
