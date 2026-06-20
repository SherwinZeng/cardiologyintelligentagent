from __future__ import annotations

import copy

import re

from typing import Any

from langchain_core.messages import AIMessage, HumanMessage

from cardiology_chat.graph.llm.keywords import has_keyword
from cardiology_chat.graph.llm.messages import all_user_text, latest_user_message
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.fallback import MEDICAL_DISCLAIMER_SHORT, is_er_doubt_question
from cardiology_chat.prompts.symptom import RED_FLAG_KEYWORDS

_NAME_RE = re.compile(
    r"(?:我叫|我的名字是|我的名字叫|叫我|你可以叫我|称呼我为|称呼我|我是)"
    r"([^\s，,。！!？?、；;：:]{1,12})"
)
_AGE_RE = re.compile(r"(?:我今年|今年|年龄|我)?\s*(\d{1,3})\s*(?:周岁|岁)")
_MALE_RE = re.compile(r"(?:我是|本人是|性别(?:是|为|:|：)?|我是一个)\s*(?:男性|男生|男士|男的|男)")
_FEMALE_RE = re.compile(r"(?:我是|本人是|性别(?:是|为|:|：)?|我是一个)\s*(?:女性|女生|女士|女的|女)")
_SUDDEN_ONSET_MARKERS = ("突然", "突发", "突然间", "一下子", "忽然")
_PRESSURE_PAIN_MARKERS = ("压榨", "压迫", "胸口被压", "胸口压")
_IDENTITY_RECALL_MARKERS = ("我叫什么", "你知道我叫什么", "你知道我的名字", "我的名字是什么", "怎么称呼我",
                            "你怎么称呼我", "叫我什么",)
_NOT_A_NAME_MARKERS = ("高血压", "糖尿病", "冠心病", "心梗", "心肌梗死", "心脏", "胸口", "胸痛", "胸闷", "男性", "女性",
                       "男的", "女的",
                       "医生", "患者", "铭铭", "助手", "什么", "谁", "哪位")

# 慢性病代号 → 高置信别名；后续可换成 LLM structured extraction 填同一结构
_CONDITION_ALIASES = {
    "hypertension": ("高血压", "血压高", "降压药"),
    "diabetes_mellitus": ("糖尿病", "血糖高", "二甲双胍", "胰岛素"),
    "dyslipidemia": ("血脂异常", "高血脂", "胆固醇高", "甘油三酯高", "他汀"),
    "prior_cad": ("冠心病", "冠脉", "心绞痛", "支架", "搭桥"),
    "prior_mi": ("心梗", "心肌梗死", "心肌梗塞"),
}


class MemoryExtractor:
    @classmethod
    def commit(cls, state: CardiologyState, text: str) -> dict:
        if not text or not text.strip():
            return {}
        # 在副本上改，最后和原 state 比较是否有变化
        structured = copy.deepcopy(state.get("structured_memory") or {})
        profile = structured.setdefault("profile", {})
        medical_profile = structured.setdefault("medical_profile", {})
        flat_memory = dict(state.get("conversation_memory") or {})
        updates: dict[str, Any] = {}
        name_match = (
            None
            if any(marker in text for marker in _IDENTITY_RECALL_MARKERS)
            else _NAME_RE.search(text.strip())
        )
        if name_match:
            name = cls._clean_name(name_match.group(1))  # 去掉语气词尾巴
            if name and not any(marker in name for marker in _NOT_A_NAME_MARKERS):
                profile["display_name"] = cls._fact(name, text)  # 带 evidence 的结构化 fact
                flat_memory["display_name"] = name
                updates["user_display_name"] = name  # 顶层字段给 greeting 等读

        age_match = _AGE_RE.search(text)
        if age_match:
            age = int(age_match.group(1))
            if 1 <= age <= 120:  # 合理年龄范围
                profile["age"] = cls._fact(age, text)
                flat_memory["age"] = str(age)

        sex = cls._extract_sex(text)
        if sex:
            profile["sex"] = cls._fact(sex, text)
            flat_memory["sex"] = sex

        # 慢性病：命中别名且非否定 → 写 chronic_conditions + state 布尔字段
        chronic_conditions = dict(medical_profile.get("chronic_conditions") or {})
        for code, aliases in _CONDITION_ALIASES.items():
            if has_keyword(text, aliases, negation_aware=True):
                chronic_conditions[code] = cls._fact(True, text)
                updates[code] = True  # hypertension / diabetes_mellitus 等
        if chronic_conditions:
            medical_profile["chronic_conditions"] = chronic_conditions
            flat_memory["chronic_conditions"] = ",".join(sorted(chronic_conditions))
        if flat_memory != (state.get("conversation_memory") or {}):
            updates["conversation_memory"] = flat_memory
        if structured != (state.get("structured_memory") or {}):
            updates["structured_memory"] = structured
        return updates

    @staticmethod
    def _clean_name(raw: str) -> str:
        return raw.strip().strip("呀啊哦噢呢啦哈嘛吧了的～~")

    @staticmethod
    def _fact(value: Any, evidence: str, confidence: str = "high") -> dict[str, Any]:
        return {"value": value, "confidence": confidence, "evidence": evidence[:80]}

    @staticmethod
    def _extract_sex(text: str) -> str | None:
        stripped = text.strip()
        if stripped in {"女性", "女生", "女士", "女的", "女"}:
            return "female"
        if stripped in {"男性", "男生", "男士", "男的", "男"}:
            return "male"
        if _FEMALE_RE.search(stripped):
            return "female"
        if _MALE_RE.search(stripped):
            return "male"
        return None


class DialoguePolicy:
    """策略层决定「这轮怎么回」；route 只决定「进哪个业务节点」。"""

    @classmethod
    def resolve(cls, state: CardiologyState, route: str, text: str) -> str:
        if cls.is_identity_recall(text):
            return "identity_recall"
        if cls.is_high_risk_context(state):
            return "er_doubt_after_high_risk" if is_er_doubt_question(text) else "emergency_red"
        if route == "symptom":
            return "symptom_intake"
        if route == "lab":
            return "lab_interpret"
        if route == "medication":
            return "medication_education"
        if route == "history":
            return "history_intake"
        if route == "greeting":
            return "small_chat"
        return "off_topic_or_fallback"

    @staticmethod
    def is_identity_recall(text: str) -> bool:
        return any(marker in text for marker in _IDENTITY_RECALL_MARKERS)

    @staticmethod
    def is_high_risk_context(state: CardiologyState) -> bool:
        if state.get("red_flag_suspected") or state.get("triage_level") == "red":
            return True
        text = all_user_text(state)  # 看整段用户发言，不单看最后一句
        if has_keyword(text, RED_FLAG_KEYWORDS, negation_aware=False):
            return True
        # ACS 典型组合：突发 + 压榨/压迫感
        return has_keyword(text, _SUDDEN_ONSET_MARKERS, negation_aware=False) and has_keyword(
            text,
            _PRESSURE_PAIN_MARKERS,
            negation_aware=False,
        )


class ContextBuilder:
    @classmethod
    def build(cls, state: CardiologyState, route: str, policy: str) -> dict[str, object]:
        structured = state.get("structured_memory") or {}
        profile = structured.get("profile") if isinstance(structured, dict) else {}
        profile = profile if isinstance(profile, dict) else {}
        display_name = cls._fact_value(profile.get("display_name")) or state.get("user_display_name")
        age = cls._fact_value(profile.get("age"))
        sex = cls._fact_value(profile.get("sex"))

        user_profile = {
            "display_name": display_name or "",
            "age": age or "",
            "sex": sex or "",
            "chronic_conditions": cls._chronic_conditions(state),
        }
        episode = cls._active_symptom_summary(state)
        return {
            "policy": policy,  # 本轮对话策略名
            "route": route,  # 图分支 route
            "user_profile_memory": user_profile,
            "active_symptom_summary": episode,
            "recent_dialogue": cls._recent_dialogue(state),
            "current_user_message": latest_user_message(state),
        }

    @classmethod
    def as_system_prompt(cls, state: CardiologyState) -> str:
        bundle = state.get("context_bundle") or {}
        if not bundle:
            bundle = cls.build(state, state.get("route") or "fallback", state.get("dialogue_policy") or "")

        profile = bundle.get("user_profile_memory") or {}
        episode = bundle.get("active_symptom_summary") or {}
        lines = ["【结构化上下文】"]  # 拼进 system prompt 的固定标题
        if isinstance(profile, dict) and any(profile.values()):
            lines.append("用户画像：")
            if profile.get("display_name"):
                lines.append(f"- 称呼：{profile['display_name']}")
            if profile.get("age"):
                lines.append(f"- 年龄：{profile['age']}岁")
            if profile.get("sex"):
                lines.append(f"- 性别：{profile['sex']}")
            if profile.get("chronic_conditions"):
                lines.append(f"- 已知基础病/危险因素：{', '.join(profile['chronic_conditions'])}")

        if isinstance(episode, dict) and any(episode.values()):
            lines.append("当前症状事件：")
            for label, key in (
                    ("主诉", "chief_complaint"),
                    ("性质", "symptom_character"),
                    ("起病", "symptom_onset"),
                    ("缓解情况", "relieving_factors"),
                    ("分诊", "triage_level"),
                    ("红旗", "red_flag_suspected"),
            ):
                value = episode.get(key)
                if value not in ("", None, False):  # 空字段不展示，减少 token
                    lines.append(f"- {label}：{value}")

        policy = bundle.get("policy")
        if policy:
            lines.append(f"本轮对话策略：{policy}")
        lines.append("请优先遵守结构化上下文；不要和用户已明确提供的信息冲突。")
        return "\n".join(lines)

    @staticmethod
    def _fact_value(fact: object) -> object:
        # 兼容 dict fact 与裸值
        if isinstance(fact, dict):
            return fact.get("value")
        return fact

    @staticmethod
    def _chronic_conditions(state: CardiologyState) -> list[str]:
        labels = []
        for key, label in (
                ("hypertension", "高血压"),
                ("diabetes_mellitus", "糖尿病"),
                ("dyslipidemia", "血脂异常"),
                ("prior_cad", "冠心病"),
                ("prior_mi", "心肌梗死史"),
        ):
            if state.get(key):
                labels.append(label)
        return labels

    @staticmethod
    def _active_symptom_summary(state: CardiologyState) -> dict[str, object]:
        text = all_user_text(state)
        return {
            "chief_complaint": state.get("chief_complaint") or "",
            "symptom_character": state.get("symptom_character")
                                 or ("压榨感/压迫感" if has_keyword(text, _PRESSURE_PAIN_MARKERS,
                                                                    negation_aware=False) else ""),
            "symptom_onset": state.get("symptom_onset")
                             or ("突然发生" if has_keyword(text, _SUDDEN_ONSET_MARKERS, negation_aware=False) else ""),
            "relieving_factors": state.get("relieving_factors")
                                 or ("部分缓解" if any(
                marker in text for marker in ("好多了", "缓解", "减轻")) else ""),
            "triage_level": state.get("triage_level") or "",
            "red_flag_suspected": bool(state.get("red_flag_suspected")),
        }

    @staticmethod
    def _recent_dialogue(state: CardiologyState, max_messages: int = 8) -> list[dict[str, str]]:
        dialogue = []
        for message in state.get("messages", [])[-max_messages:]:  # 只取最近 N 条
            if isinstance(message, HumanMessage):
                dialogue.append({"role": "user", "content": str(message.content)[:400]})
            elif isinstance(message, AIMessage):
                dialogue.append({"role": "assistant", "content": str(message.content)[:240]})
        return dialogue


def build_identity_recall_output(state: CardiologyState) -> dict:
    structured = state.get("structured_memory") or {}
    profile = structured.get("profile") if isinstance(structured, dict) else {}
    profile = profile if isinstance(profile, dict) else {}
    fact = profile.get("display_name")
    name = fact.get("value") if isinstance(fact, dict) else None
    # 多层回退：structured → user_display_name → conversation_memory
    name = name or state.get("user_display_name") or (state.get("conversation_memory") or {}).get("display_name")
    if name:
        explanation = f"你刚才告诉我，你叫{name}。我会在这段对话里用这个称呼记住你。"
    else:
        explanation = "你还没有告诉我怎么称呼你，所以我现在不能瞎猜。你可以说“我叫铭铭”，我会记住。"
    return {
        "triage_level": "green",
        "clinical_impression": explanation,
        "management_advice": "如果你愿意，可以继续告诉我想聊的心血管健康问题，我会帮你一起梳理。",
        "medical_disclaimer": MEDICAL_DISCLAIMER_SHORT,
    }
