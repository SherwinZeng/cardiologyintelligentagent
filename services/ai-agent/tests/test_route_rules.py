import unittest

from langchain_core.messages import HumanMessage

from cardiology_chat.graph.routing.rules import DEFAULT_ROUTE, resolve_route
from cardiology_chat.graph.state import empty_cardiology_state


def _state(*user_messages: str):
    state = empty_cardiology_state()
    state["messages"] = [HumanMessage(content=message) for message in user_messages]
    return state


class RouteRulesTests(unittest.TestCase):
    def test_name_intro_routes_greeting(self):
        route = resolve_route(_state("你好我叫小曾"))
        self.assertEqual(route, "greeting")

    def test_pure_hello_routes_greeting(self):
        route = resolve_route(_state("你好呀"))
        self.assertEqual(route, "greeting")

    def test_symptom_keyword_routes_symptom(self):
        route = resolve_route(_state("最近胸口闷"))
        self.assertEqual(route, "symptom")

    def test_symptom_followup_keeps_symptom_context(self):
        route = resolve_route(_state("我胸口闷", "你还记得吗"))
        self.assertEqual(route, "symptom")

    def test_er_doubt_after_red_triage_stays_symptom(self):
        state = _state("我心脏不舒服", "压榨感", "突然发生现在好多了", "一定要去吗")
        state["chief_complaint"] = "我心脏不舒服"
        state["triage_level"] = "red"
        route = resolve_route(state)
        self.assertEqual(route, "symptom")

    def test_short_symptom_detail_stays_symptom(self):
        state = _state("我心脏不舒服", "压榨感")
        state["chief_complaint"] = "我心脏不舒服"
        route = resolve_route(state)
        self.assertEqual(route, "symptom")

    def test_lab_keyword_routes_lab(self):
        route = resolve_route(_state("帮我看看这份心电图"))
        self.assertEqual(route, "lab")

    def test_ambiguous_text_defaults_symptom(self):
        route = resolve_route(_state("有点担心"))
        self.assertEqual(route, DEFAULT_ROUTE)

    def test_sticky_symptom_route_on_second_turn(self):
        state = _state("我心脏不舒服", "压榨感")
        state["route"] = "symptom"
        route = resolve_route(state)
        self.assertEqual(route, "symptom")

    def test_symptom_context_blocks_greeting_interrupt(self):
        state = _state("我心脏不舒服", "你好")
        state["chief_complaint"] = "我心脏不舒服"
        state["route"] = "symptom"
        route = resolve_route(state)
        self.assertEqual(route, "symptom")


if __name__ == "__main__":
    unittest.main()
