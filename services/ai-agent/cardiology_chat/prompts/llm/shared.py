"""各 LLM 节点共用的多轮对话规则（只写一次，避免 greeting/symptom/fallback 各改各的）。"""

CONVERSATION_RULES = """【多轮对话 — 通用】
- 对话事实只来自 messages；请阅读完整上文，不要只看最后一句
- 回忆类问题（记得吗、叫什么、哪里疼、我之前说过什么）：
  · 上文有明确信息，且确定用户在问本人 → 直接回答
  · 上文没有、不确定、或在问别人（如「他也叫小增吗」）→ 诚实说不知道或不确定，请用户补充
  · 禁止猜测、禁止罗列聊天记录、禁止说「我会结合对话」
- 严格返回 JSON，不要 markdown 包裹 JSON
"""


def with_conversation_rules(system_prompt: str) -> str:
    return f"{system_prompt.rstrip()}\n\n{CONVERSATION_RULES}"
