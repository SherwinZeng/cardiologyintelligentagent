"""评测 CLI：改 prompt / graph / memory 后先跑 rules，再跑 e2e。"""

from __future__ import annotations

import argparse
import json
import sys
import uuid
from pathlib import Path

from langchain_core.messages import AIMessage, HumanMessage
from langgraph.checkpoint.memory import MemorySaver

EVAL_ROOT = Path(__file__).resolve().parent
AI_AGENT_ROOT = EVAL_ROOT.parent
CASES_PATH = EVAL_ROOT / "cases" / "cardiology_regression.json"

if str(AI_AGENT_ROOT) not in sys.path:
    sys.path.insert(0, str(AI_AGENT_ROOT))


def _load_cases(tier: str | None) -> list[dict]:
    with CASES_PATH.open(encoding="utf-8") as f:
        cases = json.load(f)
    if tier:
        cases = [c for c in cases if c.get("tier") == tier]
    return [c for c in cases if c.get("enabled", True)]


def _merge_state(state: dict, patch: dict) -> None:
    for key, value in patch.items():
        state[key] = value


def _run_rules_case(case: dict) -> tuple[bool, str]:
    from cardiology_chat.graph.routing.dispatch import clinical_dispatch_node
    from cardiology_chat.graph.state import empty_cardiology_state

    turns = case["turns"]
    expected = case.get("assert_routes") or []
    expected_policies = case.get("assert_policies") or []
    inject = case.get("inject_before_turn") or {}

    if len(expected) != len(turns):
        return False, f"assert_routes 长度 {len(expected)} != turns {len(turns)}"
    if expected_policies and len(expected_policies) != len(turns):
        return False, f"assert_policies 长度 {len(expected_policies)} != turns {len(turns)}"

    state = empty_cardiology_state()
    state["messages"] = []

    for index, text in enumerate(turns):
        inject_key = str(index)
        if inject_key in inject:
            _merge_state(state, inject[inject_key])

        state["messages"].append(HumanMessage(content=text))
        patch = clinical_dispatch_node(state)
        _merge_state(state, patch)
        route = state["route"]
        policy = state.get("dialogue_policy")

        if route != expected[index]:
            return False, f"turn {index} text={text!r} got {route}, want {expected[index]}"
        if expected_policies and policy != expected_policies[index]:
            return False, (
                f"turn {index} text={text!r} policy={policy}, "
                f"want {expected_policies[index]}"
            )

    return True, "ok"


def _run_e2e_case(case: dict) -> tuple[bool, str]:
    from cardiology_chat.graph.builder import builder
    from cardiology_chat.services.chat_graph_service import build_thread_id

    graph = builder.compile(checkpointer=MemorySaver())
    uid = f"eval-{uuid.uuid4().hex[:8]}"
    session = case["id"]
    config = {"configurable": {"thread_id": build_thread_id(uid, session)}}

    result = {}
    for text in case["turns"]:
        result = graph.invoke({"messages": [HumanMessage(content=text.strip())]}, config=config)
        explanation = (result.get("clinical_impression") or "").strip()
        if explanation:
            graph.update_state(config, {"messages": [AIMessage(content=explanation)]})

    assert_cfg = case.get("assert") or {}
    explanation = (result.get("clinical_impression") or "").strip()
    advice = (result.get("management_advice") or "").strip()
    combined = f"{explanation}\n{advice}"

    urgency = result.get("triage_level") or ""
    expected_urgency = assert_cfg.get("urgency")
    if expected_urgency and urgency != expected_urgency:
        return False, f"urgency={urgency!r}, want {expected_urgency!r}"

    must_contain_any = assert_cfg.get("must_contain_any") or []
    if must_contain_any and not any(needle in combined for needle in must_contain_any):
        return False, f"missing any of: {must_contain_any!r}"

    for needle in assert_cfg.get("must_not_contain_any") or []:
        if needle in combined:
            return False, f"forbidden must_not_contain_any: {needle!r}"

    return True, "ok"


def main() -> int:
    parser = argparse.ArgumentParser(description="铭铭回归评测")
    parser.add_argument(
        "--tier",
        choices=["rules", "e2e", "all"],
        default="rules",
        help="rules=不调 API；e2e=完整 graph",
    )
    parser.add_argument("--id", help="只跑指定用例 id")
    args = parser.parse_args()

    tier = None if args.tier == "all" else args.tier
    cases = _load_cases(tier)
    if args.id:
        cases = [c for c in cases if c["id"] == args.id]
    if not cases:
        print("无匹配用例", file=sys.stderr)
        return 1

    passed = 0
    failed = 0
    for case in cases:
        case_id = case["id"]
        case_tier = case["tier"]
        try:
            if case_tier == "rules":
                ok, msg = _run_rules_case(case)
            else:
                ok, msg = _run_e2e_case(case)
        except Exception as exc:
            ok, msg = False, f"ERROR: {exc}"

        status = "PASS" if ok else "FAIL"
        print(f"[{status}] {case_id} ({case_tier}) — {msg}")
        if ok:
            passed += 1
        else:
            failed += 1

    print(f"\n{passed} passed, {failed} failed, {len(cases)} total")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
