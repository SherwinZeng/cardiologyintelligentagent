"""意图路由层：每轮对话的第一步。

clinical_dispatch_node 仅走规则路由（无 Router LLM）。
"""

from cardiology_chat.graph.routing.dispatch import clinical_dispatch_node, route_after_dispatch
from cardiology_chat.graph.routing.rules import resolve_route, resolve_route_by_rules

__all__ = [
    "clinical_dispatch_node",
    "route_after_dispatch",
    "resolve_route",
    "resolve_route_by_rules",
]
