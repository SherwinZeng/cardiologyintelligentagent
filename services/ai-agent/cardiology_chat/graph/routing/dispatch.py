"""clinical_dispatch_node：图的第一个节点，负责意图路由。

执行顺序：
  1. 空消息 → fallback
  2. 调用 LLM Router（多轮上下文，temperature=0.1，只返回 route 字段）
  3. LLM 失败或 route 非法 → 宽口径默认 symptom

输出：{"route": "symptom|history|..."}，供 builder 的 conditional_edges 使用。
Prompt 维护入口：prompts/llm/router_llm.py
"""

from cardiology_chat.graph.llm import invoke_llm_json, latest_user_message
from cardiology_chat.graph.state import CardiologyState
from cardiology_chat.prompts.llm.router_llm import ROUTER_LLM_SYSTEM, VALID_ROUTES

ROUTER_LLM_KEYS = ("route",)

DEFAULT_ROUTE = "symptom"


def route_after_dispatch(state: CardiologyState) -> str:
    """conditional_edges 回调：返回分支名，必须与 builder 里映射 key 一致。"""
    return state["route"]


def _normalize_route(route: str | None) -> str:
    if route in VALID_ROUTES:
        return route
    return DEFAULT_ROUTE


def _resolve_route_with_llm(state: CardiologyState) -> str:
    llm_data = invoke_llm_json(
        state,
        ROUTER_LLM_SYSTEM,
        user_text=None,
        required_keys=ROUTER_LLM_KEYS,
        temperature=0.1,
    )
    route = llm_data.get("route") if llm_data else None
    if isinstance(route, str):
        return _normalize_route(route.strip())
    return DEFAULT_ROUTE


def clinical_dispatch_node(state: CardiologyState) -> dict:
    """图入口节点：解析用户意图，写入 state['route'] 供 conditional_edges 分支。"""
    text = latest_user_message(state)
    if not text:
        return {"route": "fallback"}
    route = _resolve_route_with_llm(state)
    return {"route": route}
