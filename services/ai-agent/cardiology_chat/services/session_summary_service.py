import json
import re

from langchain_core.messages import HumanMessage, SystemMessage

from cardiology_chat.factory.LLMFactory import LLMModelFactory
from cardiology_chat.graph.llm.json_parser import parse_llm_json
from log.logger import get_app_logger

logger = get_app_logger()


def summarize_session(messages: list[dict], message_count: int | None = None) -> dict:
    normalized_messages = _normalize_messages(messages)
    fallback = _fallback_summary(normalized_messages)
    try:
        llm = LLMModelFactory().Get_LLM_Model(
            LLM="deepseek",
            model="deepseek-v4-flash",
            temperature=0.2,
            max_tokens=600,
        )
        response = llm.invoke(
            [
                SystemMessage(content=_summary_system_prompt()),
                HumanMessage(content=_summary_user_prompt(normalized_messages, message_count)),
            ]
        )
        data = parse_llm_json(getattr(response, "content", "") or "")
        title = _normalize_title(data.get("title") or fallback["title"])
        urgency = _normalize_urgency(data.get("urgency") or fallback["urgency"])
        summary = _limit_text(data.get("summary") or fallback["summary"], 1000)
        if not summary:
            summary = fallback["summary"]
        return {"title": title, "urgency": urgency, "summary": summary}
    except Exception as exc:
        logger.warning("session-summary LLM failed, fallback used: %s", exc)
        return fallback


def _summary_system_prompt() -> str:
    return (
        "你是心血管问诊记录整理助手。请只输出 JSON，不要 Markdown。"
        "任务：根据多轮对话生成问诊记录列表所需字段。"
        "字段：title（不超过6个汉字）、urgency（green/yellow/red）、summary（80到160字中文摘要）。"
        "title 必须是具体主题，如胸痛咨询、血压管理、心悸咨询、报告解读、用药咨询。"
        "summary 只概括用户主要问题、风险点和已给出的就医建议，不新增诊断。"
    )


def _summary_user_prompt(messages: list[dict], message_count: int | None) -> str:
    turns = []
    for item in messages:
        role = "用户" if item["role"] == "user" else "铭铭"
        extra = f" urgency={item['urgency']}" if item.get("urgency") else ""
        turns.append(f"{role}{extra}: {item['content']}")
    return json.dumps(
        {
            "message_count": message_count or len(messages),
            "dialogue": "\n".join(turns),
            "output_schema": {"title": "不超过6个汉字", "urgency": "green/yellow/red", "summary": "中文摘要"},
        },
        ensure_ascii=False,
    )


def _normalize_messages(messages: list[dict]) -> list[dict]:
    normalized = []
    for item in messages or []:
        role = str(item.get("role") or "").strip().lower()
        if role not in {"user", "assistant"}:
            continue
        content = _limit_text(str(item.get("content") or "").strip(), 800)
        if not content:
            continue
        normalized.append(
            {
                "role": role,
                "content": content,
                "urgency": _normalize_urgency(item.get("urgency")),
            }
        )
    return normalized


def _fallback_summary(messages: list[dict]) -> dict:
    user_text = "；".join(item["content"] for item in messages if item["role"] == "user")
    title = _guess_title(user_text)
    urgency = _highest_urgency(messages)
    if user_text:
        summary = _limit_text(f"用户主要咨询：{user_text}。已结合心血管风险进行解释，并给出观察、复诊或急诊就医建议。", 1000)
    else:
        summary = "用户完成了一次心血管健康咨询，建议结合症状变化继续观察，如不适持续或加重应及时就医。"
    return {"title": title, "urgency": urgency, "summary": summary}


def _guess_title(text: str) -> str:
    if _contains_any(text, "胸痛", "胸闷", "压榨"):
        return "胸痛咨询"
    if _contains_any(text, "血压", "高血压"):
        return "血压管理"
    if _contains_any(text, "心慌", "心悸"):
        return "心悸咨询"
    if _contains_any(text, "心电图", "ST", "QRS", "报告"):
        return "报告解读"
    if _contains_any(text, "药", "用药", "阿司匹林", "他汀"):
        return "用药咨询"
    return "心血管咨询"


def _highest_urgency(messages: list[dict]) -> str:
    has_yellow = False
    for item in messages:
        urgency = _normalize_urgency(item.get("urgency"))
        if urgency == "red":
            return "red"
        if urgency == "yellow":
            has_yellow = True
    return "yellow" if has_yellow else "green"


def _normalize_title(title: str) -> str:
    value = re.sub(r"[\s，。！？,.!?:：；;“”\"'《》<>【】\[\]（）()]+", "", str(title or ""))
    if not value:
        value = "问诊记录"
    return value[:6]


def _normalize_urgency(urgency: str | None) -> str:
    value = str(urgency or "").strip().lower()
    if value in {"red", "high"}:
        return "red"
    if value in {"yellow", "moderate", "medium"}:
        return "yellow"
    return "green"


def _contains_any(text: str, *keywords: str) -> bool:
    return any(keyword in text for keyword in keywords)


def _limit_text(value: str, max_length: int) -> str:
    return value if len(value) <= max_length else value[:max_length]
