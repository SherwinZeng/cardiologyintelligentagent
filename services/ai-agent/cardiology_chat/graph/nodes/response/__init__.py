"""直接回复类节点：寒暄 / 宽口径 fallback。"""

from cardiology_chat.graph.nodes.response.fallback import medical_fallback_response_node
from cardiology_chat.graph.nodes.response.greeting import greeting_response_node

__all__ = ["greeting_response_node", "medical_fallback_response_node"]
