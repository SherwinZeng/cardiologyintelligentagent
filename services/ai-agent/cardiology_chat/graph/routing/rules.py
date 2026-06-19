from langchain_core.messages import HumanMessage

from cardiology_chat.graph.llm import all_user_text, has_keyword, latest_user_message
from cardiology_chat.graph.routing.keywords import (
    GREETING_MARKERS,
    HISTORY_KEYWORDS,
    LAB_KEYWORDS,
    MEDICATION_KEYWORDS,
    OFF_TOPIC_KEYWORDS,
    PURE_GREETING_RE,
    SYMPTOM_FOLLOWUP_MARKERS,
    SYMPTOM_KEYWORDS,
)
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.lab import URGENT_LAB_KEYWORDS
from cardiology_chat.prompts.symptom import RED_FLAG_KEYWORDS

VALID_ROUTES = frozenset({"symptom", "history", "lab", "medication", "greeting", "fallback"})
DEFAULT_ROUTE = "symptom"
STICKY_ROUTES = frozenset({"symptom", "history", "medication", "lab"})


def resolve_route(state: CardiologyState) -> str:
    """按优先级选 route；永远返回合法 route。"""
    text = latest_user_message(state).strip()
    if not text:
        return "fallback"

    previous_route = state.get("route")
    human_turns = sum(
        1 for message in state.get("messages", []) if isinstance(message, HumanMessage)
    )
    symptom_context = (
        bool(state.get("chief_complaint"))
        or bool(state.get("red_flag_suspected"))
        or state.get("triage_level") in ("red", "yellow")
        or has_keyword(
            all_user_text(state),
            SYMPTOM_KEYWORDS + RED_FLAG_KEYWORDS,
            negation_aware=False,
        )
    )
    lab_context = (
        bool(state.get("investigation_text"))
        or bool(state.get("investigation_summary"))
        or state.get("lab_followup_needed") is True
    )

    text_lower = text.lower()
    pure_greeting = bool(PURE_GREETING_RE.match(text))
    greeting_intent = pure_greeting or any(marker in text for marker in GREETING_MARKERS)
    lab_intent = has_keyword(
        text,
        LAB_KEYWORDS + URGENT_LAB_KEYWORDS,
        case_insensitive=True,
        negation_aware=False,
    )
    medication_intent = has_keyword(
        text, MEDICATION_KEYWORDS, case_insensitive=True, negation_aware=False
    )
    history_intent = has_keyword(text, HISTORY_KEYWORDS, negation_aware=False)
    symptom_intent = has_keyword(
        text, SYMPTOM_KEYWORDS + RED_FLAG_KEYWORDS, negation_aware=False
    )
    off_topic = has_keyword(
        text, OFF_TOPIC_KEYWORDS, case_insensitive=True, negation_aware=False
    )
    if not off_topic:
        if has_keyword(text_lower, LAB_KEYWORDS, case_insensitive=True, negation_aware=False):
            off_topic = False
        elif has_keyword(text, SYMPTOM_KEYWORDS, negation_aware=False):
            off_topic = False

    symptom_followup = symptom_context and any(
        marker in text for marker in SYMPTOM_FOLLOWUP_MARKERS
    )
    continuing_symptom = (
        symptom_context
        and not greeting_intent
        and not lab_intent
        and not medication_intent
        and not history_intent
        and not off_topic
    )

    if symptom_followup or continuing_symptom:
        return "symptom"
    if lab_context and not pure_greeting:
        return "lab"
    if greeting_intent and not symptom_context:
        return "greeting"
    if lab_intent:
        return "lab"
    if medication_intent:
        return "medication"
    if history_intent:
        return "history"
    if off_topic:
        return "fallback"
    if symptom_intent:
        return "symptom"
    if (
        previous_route in STICKY_ROUTES
        and human_turns >= 2
        and (previous_route != "lab" or lab_context)
    ):
        return previous_route
    return DEFAULT_ROUTE


def resolve_route_by_rules(state: CardiologyState) -> str:
    return resolve_route(state)


def is_valid_route(route: str | None) -> bool:
    return route in VALID_ROUTES
