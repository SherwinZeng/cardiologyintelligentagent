"""意图路由层：每轮对话的第一步。

clinical_dispatch_node 写入 state["route"]，
builder 里 route_after_dispatch 读取该字段决定走哪个业务节点。

路由完全由 LLM 根据多轮 messages 判定；LLM 失败时宽口径默认 symptom。
"""

from cardiology_chat.graph.routing.dispatch import clinical_dispatch_node, route_after_dispatch

__all__ = ["clinical_dispatch_node", "route_after_dispatch"]
