"""LangGraph 图入口。

对外只暴露编译好的图实例 cardiology_graph，由 services/chat_graph_service.py 调用。

目录速览：
  builder.py   — 注册节点与边，compile 成可执行图
  state.py     — 图状态 CardiologyState（多轮对话上下文 + 输出字段）
  routing/     — 每轮第一步：意图路由（LLM 选 downstream 节点）
  llm/         — 公共工具：读消息、调 Flash、解析 JSON
  nodes/       — 业务节点，按职责分 intake / diagnostics / response
"""

from cardiology_chat.graph.builder import get_cardiology_graph

__all__ = ["get_cardiology_graph", "cardiology_graph"]


def __getattr__(name: str):
    if name == "cardiology_graph":
        return get_cardiology_graph()
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
