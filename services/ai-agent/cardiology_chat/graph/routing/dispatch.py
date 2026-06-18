"""clinical_dispatch_node：图的第一个节点，纯规则意图路由（不调 LLM）。

执行顺序：
  1. 空消息 → fallback
  2. resolve_route（规则 + 会话 sticky）
  3. 写入 state['route'] 供 conditional_edges 分支
"""

import logging

from cardiology_chat.graph.llm import latest_user_message
from cardiology_chat.graph.routing.rules import resolve_route
from cardiology_chat.graph.state import CardiologyState

logger = logging.getLogger(__name__)


def route_after_dispatch(state: CardiologyState) -> str:
    """conditional_edges 回调：返回分支名，必须与 builder 里映射 key 一致。"""
    return state["route"]


def clinical_dispatch_node(state: CardiologyState) -> dict:
    """图入口节点：解析用户意图，写入 state['route'] 供 conditional_edges 分支。"""
    text = latest_user_message(state)
    if not text:
        return {"route": "fallback"}

    route = resolve_route(state)
    logger.info("dispatch 规则路由 | route=%s text=%s", route, text[:40])
    return {"route": route}
