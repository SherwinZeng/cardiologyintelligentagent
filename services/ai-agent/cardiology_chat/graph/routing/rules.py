"""规则路由：所有图分支边仅由确定性规则决定，不调 Router LLM。"""

import re

from langchain_core.messages import HumanMessage

from cardiology_chat.graph.llm import all_user_text, has_keyword, latest_user_message
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.fallback import (
    _AUTHOR_KEYWORDS,
    _CREATOR_EASTER_EGG_KEYWORDS,
    _LIKE_KEYWORDS,
)
from cardiology_chat.prompts.lab import URGENT_LAB_KEYWORDS
from cardiology_chat.prompts.symptom import RED_FLAG_KEYWORDS

VALID_ROUTES = frozenset({"symptom", "history", "lab", "medication", "greeting", "fallback"})
DEFAULT_ROUTE = "symptom"
_STICKY_ROUTES = frozenset({"symptom", "history", "medication", "lab"})

_NAME_INTRO_MARKERS = ("我叫", "我的名字", "称呼我", "叫我")

_PURE_GREETING_RE = re.compile(
    r"^(你好|您好|嗨|hello|hi|在吗|早上好|晚上好|下午好)[呀啊呢哦哈啦嘛吧了！!？?。．.~～\s]*$",
    re.IGNORECASE,
)

_BOT_META_MARKERS = (
    "你是谁",
    "你叫什么",
    "你能做什么",
    "你能干什么",
    "你会什么",
    "介绍一下你",
    "你是铭铭",
)

_SYMPTOM_KEYWORDS = (
    "胸闷",
    "胸痛",
    "胸口",
    "心口",
    "心脏",
    "心慌",
    "心悸",
    "心跳",
    "气短",
    "喘不上气",
    "呼吸困难",
    "晕厥",
    "晕倒",
    "水肿",
    "不舒服",
    "压榨",
    "疼痛",
    "血压高",
    "血压低",
    "高血压",
    "低血压",
)

_SYMPTOM_FOLLOWUP_MARKERS = (
    "记得吗",
    "还记得",
    "刚才说",
    "之前说",
    "上面说",
    "哪里疼",
    "哪里痛",
    "哪里不舒服",
    "我怎么了",
    "一定要去",
    "必须去",
    "要不要去",
    "需不需要去",
    "能不能不去",
    "可以不",
    "还去吗",
    "现在去吗",
    "严重吗",
    "要紧吗",
    "危险吗",
    "会没事吗",
    "怎么办",
)

_LAB_KEYWORDS = (
    "心电图",
    "ECG",
    "Holter",
    "QRS",
    "QT",
    "ST段",
    "ST-T",
    "射血分数",
    "EF",
    "彩超",
    "心超",
    "冠脉",
    "CTA",
    "造影",
    "BNP",
    "NT-proBNP",
    "肌钙蛋白",
    "troponin",
    "血脂",
    "胆固醇",
    "LDL",
    "HDL",
    "检查报告",
    "化验",
    "检验结果",
    "报告解读",
)

_MEDICATION_KEYWORDS = (
    "用药",
    "吃药",
    "服药",
    "药物",
    "药品",
    "剂量",
    "停药",
    "换药",
    "副作用",
    "硝酸甘油",
    "阿司匹林",
    "他汀",
    "降压药",
    "倍他乐克",
    "美托洛尔",
    "ARB",
    "ACEI",
    "CCB",
    "沙坦",
    "普利",
    "地平",
)

_HISTORY_KEYWORDS = (
    "既往史",
    "病史",
    "家族史",
    "危险因素",
    "得过",
    "诊断过",
    "开过刀",
    "支架",
    "搭桥",
    "PCI",
    "CABG",
    "吸烟史",
    "抽烟",
)

_OFF_TOPIC_KEYWORDS = (
    "天气",
    "股票",
    "基金",
    "编程",
    "python",
    "java",
    "代码",
    "足球",
    "篮球",
    "明星",
    "八卦",
    "电影推荐",
)


def _normalize(text: str) -> str:
    return text.strip()


def _is_pure_greeting(text: str) -> bool:
    return bool(_PURE_GREETING_RE.match(_normalize(text)))


def _is_bot_meta(text: str) -> bool:
    lower = text.lower()
    if any(marker in text for marker in _BOT_META_MARKERS):
        return True
    if any(keyword in text for keyword in _AUTHOR_KEYWORDS):
        return True
    if any(keyword in text for keyword in _LIKE_KEYWORDS):
        return True
    if any(keyword in text for keyword in _CREATOR_EASTER_EGG_KEYWORDS):
        return True
    return "zengxiangrui" in lower


def _has_symptom_context(state: CardiologyState) -> bool:
    if state.get("chief_complaint"):
        return True
    if state.get("red_flag_suspected"):
        return True
    if state.get("triage_level") in ("red", "yellow"):
        return True
    return has_keyword(all_user_text(state), _SYMPTOM_KEYWORDS + RED_FLAG_KEYWORDS, negation_aware=False)


def _is_continuing_symptom_turn(state: CardiologyState, text: str) -> bool:
    """已有症状/分诊语境时，短追问与补充说明继续走 symptom，避免每轮重跑 LLM 路由丢上下文。"""
    if not _has_symptom_context(state):
        return False
    if _is_pure_greeting(text) or _is_bot_meta(text):
        return False
    if _is_lab_intent(text) or _is_medication_intent(text) or _is_history_intent(text):
        return False
    if _is_clearly_off_topic(text):
        return False
    return True


def _has_lab_context(state: CardiologyState) -> bool:
    if state.get("investigation_text"):
        return True
    if state.get("investigation_summary"):
        return True
    return state.get("lab_followup_needed") is True


def _is_symptom_followup(state: CardiologyState, text: str) -> bool:
    if not _has_symptom_context(state):
        return False
    return any(marker in text for marker in _SYMPTOM_FOLLOWUP_MARKERS)


def _is_lab_intent(text: str) -> bool:
    return has_keyword(
        text,
        _LAB_KEYWORDS + URGENT_LAB_KEYWORDS,
        case_insensitive=True,
        negation_aware=False,
    )


def _is_medication_intent(text: str) -> bool:
    return has_keyword(text, _MEDICATION_KEYWORDS, case_insensitive=True, negation_aware=False)


def _is_history_intent(text: str) -> bool:
    return has_keyword(text, _HISTORY_KEYWORDS, negation_aware=False)


def _is_symptom_intent(text: str) -> bool:
    return has_keyword(text, _SYMPTOM_KEYWORDS + RED_FLAG_KEYWORDS, negation_aware=False)


def _is_clearly_off_topic(text: str) -> bool:
    lower = text.lower()
    if has_keyword(text, _OFF_TOPIC_KEYWORDS, case_insensitive=True, negation_aware=False):
        return True
    if has_keyword(lower, _LAB_KEYWORDS, case_insensitive=True, negation_aware=False):
        return False
    if has_keyword(text, _SYMPTOM_KEYWORDS, negation_aware=False):
        return False
    return False


def _human_message_count(state: CardiologyState) -> int:
    return sum(
        1
        for message in state.get("messages", [])
        if isinstance(message, HumanMessage)
    )


def _is_name_intro(text: str) -> bool:
    return any(marker in text for marker in _NAME_INTRO_MARKERS)


def _resolve_sticky_route(state: CardiologyState) -> str | None:
    """同一会话内锁定 route，避免每轮重新猜意图。"""
    previous = state.get("route")
    if previous not in _STICKY_ROUTES:
        return None
    if _human_message_count(state) < 2:
        return None
    if previous == "lab" and not _has_lab_context(state):
        return None
    return previous


def resolve_route(state: CardiologyState) -> str:
    """纯规则路由：永远返回合法 route，不返回 None。"""
    text = _normalize(latest_user_message(state))
    if not text:
        return "fallback"

    if _is_symptom_followup(state, text) or _is_continuing_symptom_turn(state, text):
        return "symptom"

    if _has_lab_context(state) and not _is_pure_greeting(text):
        return "lab"

    if (_is_pure_greeting(text) or _is_bot_meta(text) or _is_name_intro(text)) and not _has_symptom_context(state):
        return "greeting"

    if _is_lab_intent(text):
        return "lab"

    if _is_medication_intent(text):
        return "medication"

    if _is_history_intent(text):
        return "history"

    if _is_clearly_off_topic(text):
        return "fallback"

    if _is_symptom_intent(text):
        return "symptom"

    sticky = _resolve_sticky_route(state)
    if sticky:
        return sticky

    return DEFAULT_ROUTE


def resolve_route_by_rules(state: CardiologyState) -> str:
    """兼容旧调用方；与 resolve_route 相同。"""
    return resolve_route(state)


def is_valid_route(route: str | None) -> bool:
    return route in VALID_ROUTES
