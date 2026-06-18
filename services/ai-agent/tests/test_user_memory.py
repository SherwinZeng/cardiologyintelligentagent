import unittest

from langchain_core.messages import HumanMessage

from cardiology_chat.graph.memory import (
    conversation_memory_node,
    is_user_name_recall,
)
from cardiology_chat.graph.nodes.response.greeting import greeting_response_node
from cardiology_chat.graph.state import empty_cardiology_state


def _state_with_user_messages(*contents: str):
    state = empty_cardiology_state()
    state["messages"] = [HumanMessage(content=content) for content in contents]
    return state


class UserMemoryTests(unittest.TestCase):
    def test_detects_name_recall_question(self):
        self.assertTrue(is_user_name_recall("你知道我叫什么名字吗"))
        self.assertTrue(is_user_name_recall("还记得我的名字吗"))
        self.assertTrue(is_user_name_recall("你怎么称呼我"))

    def test_memory_node_restores_latest_name_from_prior_history(self):
        state = _state_with_user_messages("你好我叫小曾", "你知道我叫什么名字吗")

        memory = conversation_memory_node(state)

        self.assertEqual(memory["user_display_name"], "小曾")
        self.assertEqual(memory["conversation_memory"]["user_display_name"], "小曾")

    def test_greeting_node_uses_memory_state_without_llm(self):
        state = _state_with_user_messages("你好我叫小曾", "你知道我叫什么名字吗")
        state.update(conversation_memory_node(state))

        result = greeting_response_node(state)

        self.assertEqual(result["triage_level"], "green")
        self.assertIn("小曾", result["clinical_impression"])

    def test_repeated_recall_questions_do_not_pollute_name(self):
        state = _state_with_user_messages("你好我叫小曾", "我叫什么", "我叫什么")

        memory = conversation_memory_node(state)

        self.assertEqual(memory["user_display_name"], "小曾")

    def test_how_should_you_call_me_uses_existing_name(self):
        state = _state_with_user_messages("你好我叫小曾", "我叫什么", "你怎么称呼我")
        state.update(conversation_memory_node(state))

        result = greeting_response_node(state)

        self.assertIn("小曾", result["clinical_impression"])


if __name__ == "__main__":
    unittest.main()
