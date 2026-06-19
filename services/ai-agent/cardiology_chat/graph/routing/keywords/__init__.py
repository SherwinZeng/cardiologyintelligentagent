"""路由意图关键词表（按场景分文件，rules.py 只负责优先级逻辑）。"""

from cardiology_chat.graph.routing.keywords.greeting import GREETING_MARKERS, PURE_GREETING_RE
from cardiology_chat.graph.routing.keywords.history import HISTORY_KEYWORDS
from cardiology_chat.graph.routing.keywords.lab import LAB_KEYWORDS
from cardiology_chat.graph.routing.keywords.medication import MEDICATION_KEYWORDS
from cardiology_chat.graph.routing.keywords.off_topic import OFF_TOPIC_KEYWORDS
from cardiology_chat.graph.routing.keywords.symptom import (
    SYMPTOM_FOLLOWUP_MARKERS,
    SYMPTOM_KEYWORDS,
)

__all__ = [
    "PURE_GREETING_RE",
    "GREETING_MARKERS",
    "SYMPTOM_KEYWORDS",
    "SYMPTOM_FOLLOWUP_MARKERS",
    "LAB_KEYWORDS",
    "MEDICATION_KEYWORDS",
    "HISTORY_KEYWORDS",
    "OFF_TOPIC_KEYWORDS",
]
