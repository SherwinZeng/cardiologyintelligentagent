import unittest
import os
from unittest.mock import patch

from langchain_core.messages import HumanMessage
from langgraph.checkpoint.memory import MemorySaver

from cardiology_chat.graph.dialogue_core import (
    ContextBuilder,
    DialoguePolicy,
    build_identity_recall_output,
)
from cardiology_chat.graph.builder import builder
from cardiology_chat.graph.routing.dispatch import clinical_dispatch_node
from cardiology_chat.graph.state import empty_cardiology_state
from cardiology_chat.memory.commit import commit_user_facts
from cardiology_chat.memory.extractors import extract_age, extract_display_name, extract_sex


class MemoryExtractorTests(unittest.TestCase):
    def test_extract_display_name(self):
        self.assertEqual(extract_display_name("你好我叫小曾"), "小曾")
        self.assertEqual(extract_display_name("叫我小明"), "小明")
        self.assertIsNone(extract_display_name("小曾不舒服"))

    def test_extract_age(self):
        self.assertEqual(extract_age("我今年35岁"), "35")
        self.assertIsNone(extract_age("三十五岁"))

    def test_extract_sex(self):
        self.assertEqual(extract_sex("我是男性"), "male")
        self.assertEqual(extract_sex("女性"), "female")

    def test_commit_writes_name(self):
        state = empty_cardiology_state()
        updates = commit_user_facts(state, "你好我叫小曾")
        self.assertEqual(updates["user_display_name"], "小曾")
        self.assertEqual(updates["conversation_memory"]["display_name"], "小曾")
        self.assertEqual(
            updates["structured_memory"]["profile"]["display_name"]["value"],
            "小曾",
        )

    def test_dispatch_builds_memory_policy_and_context(self):
        state = empty_cardiology_state()
        state["messages"] = [HumanMessage(content="你好我叫小曾")]

        updates = clinical_dispatch_node(state)

        self.assertEqual(updates["route"], "greeting")
        self.assertEqual(updates["dialogue_policy"], "small_chat")
        self.assertEqual(updates["user_display_name"], "小曾")
        self.assertEqual(
            updates["context_bundle"]["user_profile_memory"]["display_name"],
            "小曾",
        )

    def test_identity_recall_policy_overrides_symptom_context(self):
        state = empty_cardiology_state()
        state["messages"] = [
            HumanMessage(content="你好我叫小曾"),
            HumanMessage(content="我心脏不舒服"),
            HumanMessage(content="我叫什么"),
        ]
        state["chief_complaint"] = "我心脏不舒服"
        state["triage_level"] = "yellow"
        state["structured_memory"] = {
            "profile": {"display_name": {"value": "小曾", "confidence": "high", "evidence": "你好我叫小曾"}}
        }
        state["conversation_memory"] = {"display_name": "小曾"}
        state["user_display_name"] = "小曾"

        updates = clinical_dispatch_node(state)

        self.assertEqual(updates["route"], "greeting")
        self.assertEqual(updates["dialogue_policy"], "identity_recall")

    def test_identity_recall_output_reads_structured_memory(self):
        state = empty_cardiology_state()
        state["structured_memory"] = {
            "profile": {"display_name": {"value": "小曾", "confidence": "high", "evidence": "你好我叫小曾"}}
        }

        result = build_identity_recall_output(state)

        self.assertEqual(result["triage_level"], "green")
        self.assertIn("小曾", result["clinical_impression"])

    def test_identity_recall_without_memory_does_not_guess(self):
        state = empty_cardiology_state()

        result = build_identity_recall_output(state)

        self.assertIn("不能瞎猜", result["clinical_impression"])

    def test_context_builder_keeps_high_risk_episode(self):
        state = empty_cardiology_state()
        state["messages"] = [
            HumanMessage(content="我心脏不舒服"),
            HumanMessage(content="压榨感"),
            HumanMessage(content="突然间就这样了但现在好多了"),
            HumanMessage(content="一定要去吗"),
        ]
        state["chief_complaint"] = "我心脏不舒服"
        state["triage_level"] = "red"
        state["red_flag_suspected"] = True

        policy = DialoguePolicy.resolve(state, "symptom", "一定要去吗")
        bundle = ContextBuilder.build(state, "symptom", policy)

        self.assertEqual(policy, "er_doubt_after_high_risk")
        self.assertEqual(bundle["active_symptom_summary"]["symptom_character"], "压榨感/压迫感")
        self.assertEqual(bundle["active_symptom_summary"]["symptom_onset"], "突然发生")
        self.assertEqual(bundle["active_symptom_summary"]["triage_level"], "red")

    def test_graph_recalls_name_without_llm(self):
        graph = builder.compile(checkpointer=MemorySaver())
        config = {"configurable": {"thread_id": "unit:user-name"}}

        with patch.dict(
            os.environ,
            {"LANGCHAIN_TRACING_V2": "false", "LANGSMITH_TRACING": "false"},
        ), patch(
            "cardiology_chat.graph.llm.invoke.get_flash_llm",
            side_effect=RuntimeError("LLM should not be required for identity recall"),
        ):
            graph.invoke({"messages": [HumanMessage(content="你好我叫小曾")]}, config=config)
            result = graph.invoke({"messages": [HumanMessage(content="我叫什么")]}, config=config)

        self.assertEqual(result["dialogue_policy"], "identity_recall")
        self.assertIn("小曾", result["clinical_impression"])


if __name__ == "__main__":
    unittest.main()
