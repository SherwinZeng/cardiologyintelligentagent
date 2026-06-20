"""clinical_dispatch_node：图的第一个节点，纯规则意图路由（不调 LLM）。

执行顺序：
  1. 空消息 → fallback
  2. resolve_route（规则 + 会话 sticky）
  3. 写入 state['route'] 供 conditional_edges 分支
"""

from cardiology_chat.graph.dialogue_core import ContextBuilder, DialoguePolicy, MemoryExtractor
from cardiology_chat.graph.llm import latest_user_message
from cardiology_chat.graph.routing.rules import resolve_route
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.rag.guide_retriever import retrieve_guide_context
from log.logger import get_app_logger

logger = get_app_logger()


def route_after_dispatch(state: CardiologyState) -> str:
    return state["route"]


def clinical_dispatch_node(state: CardiologyState) -> dict:
    """图入口：先写稳定记忆，再决定 route / policy / context。"""
    text = latest_user_message(state)
    if not text:
        return {"route": "fallback"}

    memory_updates = MemoryExtractor.commit(state, text)
    decision_state = {**state, **memory_updates}
    route = resolve_route(decision_state)
    policy = DialoguePolicy.resolve(decision_state, route, text)

    # 身份回忆是确定性策略，不交给症状 sticky 或 LLM 猜。
    if policy == "identity_recall":
        route = "greeting"

    context_state = {**decision_state, "route": route, "dialogue_policy": policy}
    context_bundle = ContextBuilder.build(context_state, route, policy)
    guide_result = retrieve_guide_context(text, route)
    if guide_result.excerpts:
        context_bundle["guide_rag"] = guide_result.excerpts
        context_bundle["guide_references"] = guide_result.guide_references
        logger.info(
            "guide RAG 命中 | route=%s query=%s hits=%d guides=%s",
            route,
            text[:60],
            len(guide_result.excerpts),
            guide_result.guide_references,
        )
    else:
        logger.info(
            "guide RAG 未命中 | route=%s query=%s",
            route,
            text[:60],
        )
    structured = memory_updates.get("structured_memory") or {}
    profile = structured.get("profile") if isinstance(structured, dict) else {}
    medical_profile = structured.get("medical_profile") if isinstance(structured, dict) else {}
    episode = context_bundle.get("active_symptom_summary") or {}
    episode_active = any(value not in ("", None, False, []) for value in episode.values())
    logger.info(
        "dialogue_core 决策完成 | route=%s policy=%s memory_changed=%s "
        "profile_keys=%s medical_keys=%s episode_active=%s",
        route,
        policy,
        bool(memory_updates),
        sorted(profile.keys()) if isinstance(profile, dict) else [],
        sorted(medical_profile.keys()) if isinstance(medical_profile, dict) else [],
        episode_active,
    )
    return {
        **memory_updates,
        "route": route,
        "dialogue_policy": policy,
        "context_bundle": context_bundle,
    }
