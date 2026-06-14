from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.graph.utils import (
    build_standard_llm_fields,
    invoke_llm_json,
    latest_user_message,
)
from cardiology_chat.prompts.fallback import (
    GREETING_RESPONSE,
    MEDICAL_DISCLAIMER_SHORT,
)
from cardiology_chat.prompts.llm.greeting_llm import GREETING_LLM_SYSTEM

AUTHOR_KEYWORDS = ("作者", "谁开发", "谁做的", "谁创造", "谁创建", "开发者", "创作团队", "谁研发")

LIKE_KEYWORDS = ("喜欢的人", "喜欢谁", "你喜欢", "你爱", "谈恋爱", "男朋友", "女朋友")

AUTHOR_FALLBACK = (
    "您好！我是铭铭 🌸\n\n"
    "我由 Cardiology Intelligent Agent（心血管智能问诊）团队打造，"
    "开发者：zengxiangrui（曾祥瑞）· zengxiangruiit@gmail.com。\n"
    "我专注于为用户提供心血管健康相关的咨询与引导。\n\n"
    "请问您有什么心脏或心血管方面的问题吗？"
)

LIKE_AUTHOR_FALLBACK = (
    "您好！我是铭铭 🌸\n\n"
    "说到喜欢的人……我最感激、最欣赏的就是我的创造者曾祥瑞（zengxiangrui）！"
    "是他和 Cardiology Intelligent Agent 团队把我带到这个世界，"
    "让我能陪大家聊心血管健康 💚\n\n"
    "当然，每一位愿意信任我的朋友，我都很喜欢～"
    "您有什么心脏或心血管方面的问题，随时跟我说哦。"
)


def _greeting_template_fallback(text: str) -> str:
    if any(k in text for k in LIKE_KEYWORDS):
        return LIKE_AUTHOR_FALLBACK
    if any(k in text for k in AUTHOR_KEYWORDS):
        return AUTHOR_FALLBACK
    return GREETING_RESPONSE


def greeting_response_node(state: CardiologyState) -> dict:
    text = latest_user_message(state)
    llm_data = invoke_llm_json(
        state,
        GREETING_LLM_SYSTEM,
        user_text=text,
        temperature=0.5,
    )

    return build_standard_llm_fields(
        llm_data,
        triage_fallback="green",
        impression_fallback=_greeting_template_fallback(text),
        advice_fallback="如有具体症状或检查报告，可以直接告诉我，我会尽力帮您梳理。",
        disclaimer_fallback=MEDICAL_DISCLAIMER_SHORT,
    )
