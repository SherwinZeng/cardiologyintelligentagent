import unittest
from unittest.mock import patch

from langchain_core.messages import HumanMessage

from cardiology_chat.graph.nodes.intake import symptom as symptom_module
from cardiology_chat.graph.nodes.intake.symptom import _is_symptom_fully_resolved
from cardiology_chat.graph.state import empty_cardiology_state
from cardiology_chat.prompts.fallback import ER_DOUBT_RED_FALLBACK, resolve_symptom_static_impression


class SymptomRoutingTests(unittest.TestCase):
    def test_partial_relief_is_not_fully_resolved(self):
        state = empty_cardiology_state()
        state["triage_level"] = "red"
        self.assertFalse(_is_symptom_fully_resolved(state, "突然间就这样了但现在好多了"))

    def test_er_doubt_fallback_under_red_context(self):
        state = empty_cardiology_state()
        state["triage_level"] = "red"
        impression = resolve_symptom_static_impression(state, "一定要去吗")
        self.assertEqual(impression, ER_DOUBT_RED_FALLBACK)

    def test_er_doubt_under_red_context_uses_fast_path(self):
        state = empty_cardiology_state()
        state["messages"] = [HumanMessage(content="一定要去吗")]
        state["chief_complaint"] = "突然压榨样胸痛"
        state["triage_level"] = "red"
        state["red_flag_suspected"] = True

        with patch.object(
            symptom_module,
            "run_standard_conversation_node",
            side_effect=AssertionError("high-risk ER doubt should not call LLM"),
        ):
            result = symptom_module.symptom_collection_node(state)

        self.assertEqual(result["triage_level"], "red")
        self.assertIn("仍建议您**尽快**", result["clinical_impression"])
        self.assertIn("现在就去急诊", result["management_advice"])
        self.assertNotIn("补充完整信息", result["clinical_impression"])

    def test_er_doubt_infers_high_risk_from_history_messages(self):
        state = empty_cardiology_state()
        state["messages"] = [
            HumanMessage(content="我心脏不舒服"),
            HumanMessage(content="压榨感"),
            HumanMessage(content="突然间就这样了但现在好多了"),
            HumanMessage(content="一定要去吗"),
        ]

        with patch.object(
            symptom_module,
            "run_standard_conversation_node",
            side_effect=AssertionError("inferred high-risk ER doubt should not call LLM"),
        ):
            result = symptom_module.symptom_collection_node(state)

        self.assertEqual(result["triage_level"], "red")
        self.assertIn("不要因「现在好多了」而完全放心", result["clinical_impression"])
        self.assertIn("现在就去急诊", result["management_advice"])


if __name__ == "__main__":
    unittest.main()
