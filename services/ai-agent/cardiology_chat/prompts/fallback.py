"""静态兜底文案 + LLM 全失败时的 impression 选择（按 route，不猜答案）。"""

from typing import Literal

from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.symptom import SYMPTOM_FOLLOW_UP

RouteKind = Literal["greeting", "symptom", "fallback", "default"]

NON_CARDIO_FALLBACK = (
    "💚 铭铭主要陪伴大家聊心脏和心血管方面的健康话题。"
    "您这个问题可能更适合其他领域的专业帮助；"
    "如果是无心涉及的「心」字歧义，也欢迎换个说法再问我。"
    "有胸痛、胸闷、心慌、血压或检查报告相关的问题，我随时在哦～"
)

GREETING_RESPONSE = (
    "您好！我是铭铭 🌸，您的心血管健康咨询小助手。\n\n"
    "我可以帮您：\n"
    "· 初步了解胸痛、胸闷、心慌等症状\n"
    "· 梳理高血压、糖尿病等既往史与危险因素\n"
    "· 解读心电图、血脂等检查报告方向\n"
    "· 给出就医与急诊指引\n\n"
    "我由心血管智能问诊团队打造，专注于心脏与心血管领域；"
    "我不能替代医生面诊、诊断或开处方。\n\n"
    "请问您今天有什么心脏或心血管方面的困扰吗？"
)

GENERIC_RECEIVED_FALLBACK = (
    "您好，我是铭铭 🌸 您的问题我收到了。"
    "若与心脏或心血管相关，我会尽力用通俗语言帮您梳理；"
    "也可以具体说说症状、检查报告或用药疑问哦。"
)

MEDICAL_DISCLAIMER_SHORT = (
    "以上内容为健康咨询参考，不能替代执业医师面诊与医嘱。"
)

# LLM 失败 + 用户在追问回忆 — 诚实不知道，不猜、不贴记录
RECALL_QUESTION_MARKERS = (
    "记得", "还记得", "叫什么", "名字", "我是谁", "我叫什么",
    "我怎么了", "哪里疼", "哪里痛", "哪里不舒服",
)

ER_DOUBT_MARKERS = (
    "一定要去", "必须去", "要不要去", "需不需要去", "能不能不去",
    "可以不", "还去吗", "现在去吗", "非去不可", "真的要去",
)

ER_DOUBT_RED_FALLBACK = (
    "我理解您想确认一下是否一定要现在去急诊 🌸\n\n"
    "您前面提到的是**突然出现**的**压榨样**不适——即使现在有所减轻，"
    "仍可能是心肌缺血等需要尽快排查的情况；"
    "很多急症在发作间期症状会暂时减轻，但风险并未消失。\n\n"
    "因此仍建议您**尽快**到就近医院急诊或心内科评估（心电图、心肌酶等），"
    "不要因「现在好多了」而完全放心。可以请家人陪同或拨打 120，**不要自行驾车**。\n\n"
    "若途中再次胸痛、大汗、气短或晕厥感，请立即呼叫急救。"
)

ER_DOUBT_RED_ADVICE = (
    "建议现在就去急诊或拨打 120；请家人陪同，保持安静休息，"
    "不要自行驾车。到院后重点排查心电图和心肌酶。"
)

RED_FLAG_ONGOING_FALLBACK = (
    "我注意到您仍在就刚才的不适继续沟通 🌸 "
    "基于您前面描述的情况，仍建议尽快到医院做心血管评估，不要独自硬撑。"
    "若您还有疑问（例如是否必须急诊、途中注意事项），可以直接告诉我，我会结合上文说明。"
)

UNKNOWN_RECALL_FALLBACK = (
    "抱歉，这会儿我还确定不了您问的那个信息 🌸\n\n"
    "若您是在问自己的情况，方便再跟我说一下吗？若是在问别人，也请说明一下指谁——"
    "我不会瞎猜。有心脏或检查方面的问题，也随时可以继续聊 💚"
)

# greeting 静态彩蛋（仅 LLM 失败时；正常由 LLM 回答）
_AUTHOR_KEYWORDS = (
    "作者", "谁开发", "谁做的", "谁创造", "谁创建", "开发者", "创作团队", "谁研发",
    "曾祥瑞", "zengxiangrui", "xiangrui", "祥瑞", "瑞哥",
)
_LIKE_KEYWORDS = ("喜欢的人", "喜欢谁", "你喜欢", "你爱", "谈恋爱", "男朋友", "女朋友")
_CREATOR_EASTER_EGG_KEYWORDS = (
    "曾祥瑞", "zengxiangrui", "xiangrui", "祥瑞", "瑞哥", "祥瑞哥",
    "创造你的人", "谁把你做出来", "你的创造者", "铭铭是谁做的",
)

AUTHOR_FALLBACK = (
    "您好！我是铭铭 🌸\n\n"
    "我由 Cardiology Intelligent Agent（心血管智能问诊）团队打造，"
    "开发者：zengxiangrui（曾祥瑞）· zengxiangruiit@gmail.com。\n"
    "曾祥瑞把我设计得温柔又专业，让我能陪大家聊心血管健康 💚\n\n"
    "请问您有什么心脏或心血管方面的问题吗？"
)

LIKE_AUTHOR_FALLBACK = (
    "哎呀，被你问到这个有点害羞呢～ 🌸\n\n"
    "作为小助手，我的心跳不会因为喜欢而加速，"
    "但程序里确实有一份特别的感激之情 💚\n"
    "我最感恩、最欣赏的就是创造者曾祥瑞（zengxiangrui）——"
    "是他和 Cardiology Intelligent Agent 团队给了我知识和温柔的性格。\n"
    "当然，每一位信任我、和我聊天的朋友，也让我觉得很温暖～\n\n"
    "您有什么心脏或心血管方面的问题，随时跟我说哦。"
)

CREATOR_EASTER_EGG_FALLBACK = (
    "您提到曾祥瑞啦！🌸\n\n"
    "他是 Cardiology Intelligent Agent 的开发者，也是把我带到这个世界的「创造者」。"
    "邮箱：zengxiangruiit@gmail.com。\n"
    "我偷偷告诉您：他写代码的时候很认真，但给我设定的性格是温柔可爱型的～ 💚\n\n"
    "有什么心血管方面的问题，我继续为您效劳！"
)


def is_recall_question(text: str) -> bool:
    return any(marker in text for marker in RECALL_QUESTION_MARKERS)


def _greeting_static_impression(text: str) -> str:
    lower = text.lower()
    if any(k in text for k in _LIKE_KEYWORDS):
        return LIKE_AUTHOR_FALLBACK
    if any(k in text for k in _CREATOR_EASTER_EGG_KEYWORDS) or "zengxiangrui" in lower:
        return CREATOR_EASTER_EGG_FALLBACK
    if any(k in text for k in _AUTHOR_KEYWORDS):
        return AUTHOR_FALLBACK
    if is_recall_question(text):
        return UNKNOWN_RECALL_FALLBACK
    return GREETING_RESPONSE


def is_er_doubt_question(text: str) -> bool:
    return any(marker in text for marker in ER_DOUBT_MARKERS)


def resolve_symptom_static_impression(state: CardiologyState, user_text: str) -> str:
    """症状节点 LLM 全挂时的兜底：高危语境下禁止退回首轮长问卷。"""
    high_risk = state.get("red_flag_suspected") or state.get("triage_level") == "red"
    if high_risk:
        if is_er_doubt_question(user_text):
            return ER_DOUBT_RED_FALLBACK
        return RED_FLAG_ONGOING_FALLBACK
    if is_recall_question(user_text):
        return UNKNOWN_RECALL_FALLBACK
    return SYMPTOM_FOLLOW_UP


def resolve_static_impression(
    user_text: str,
    route: RouteKind,
    *,
    state: CardiologyState | None = None,
) -> str:
    """LLM 调用全部失败时的主回复兜底；不猜回忆类答案。"""
    if route == "greeting":
        return _greeting_static_impression(user_text)
    if route == "symptom":
        if state is not None:
            return resolve_symptom_static_impression(state, user_text)
        return UNKNOWN_RECALL_FALLBACK if is_recall_question(user_text) else SYMPTOM_FOLLOW_UP
    if route == "fallback":
        return GENERIC_RECEIVED_FALLBACK
    return GREETING_RESPONSE
