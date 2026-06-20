#!/usr/bin/env python3
"""本地验证指南 RAG 是否可用、检索结果是否合理。

用法（在 services/ai-agent 目录）：
  poetry run python scripts/test_rag_retrieval.py
  poetry run python scripts/test_rag_retrieval.py "高血压要做什么检查" --route lab
  poetry run python scripts/test_rag_retrieval.py "胸口闷痛怎么办" --route symptom --dispatch
"""

from __future__ import annotations

import argparse
import os
import sys

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, BASE_DIR)
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "configuration.settings")

import django

django.setup()

from langchain_core.messages import HumanMessage

from cardiology_chat.graph.routing.dispatch import clinical_dispatch_node
from cardiology_chat.graph.routing.rules import resolve_route
from cardiology_chat.graph.state import empty_cardiology_state
from cardiology_chat.rag.guide_retriever import is_rag_available, retrieve_guide_context


def main() -> None:
    parser = argparse.ArgumentParser(description="测试心血管指南 RAG 检索")
    parser.add_argument("query", nargs="?", default="高血压要做什么检查", help="测试问题")
    parser.add_argument(
        "--route",
        default="",
        help="强制 route（symptom/lab/medication/history）；默认自动推断",
    )
    parser.add_argument(
        "--dispatch",
        action="store_true",
        help="走 clinical_dispatch_node，模拟 agent 实际注入 context_bundle",
    )
    args = parser.parse_args()

    print(f"RAG_ENABLED + Milvus + ZHIPU: {'可用' if is_rag_available() else '不可用'}")
    if not is_rag_available():
        print("请检查 .env 中 RAG_ENABLED、ZHIPU_API_KEY、MILVUS_URI")
        sys.exit(1)

    state = empty_cardiology_state()
    state["messages"] = [HumanMessage(content=args.query)]
    route = args.route or resolve_route(state)
    print(f"query: {args.query}")
    print(f"route: {route}")

    if args.dispatch:
        result = clinical_dispatch_node(state)
        route = result.get("route") or route
        bundle = result.get("context_bundle") or {}
        excerpts = bundle.get("guide_rag") or []
        guide_refs = bundle.get("guide_references") or []
        print(f"dispatch route: {route}")
        print(f"guide_references: {guide_refs}")
    else:
        retrieval = retrieve_guide_context(args.query, route)
        excerpts = retrieval.excerpts
        print(f"guide_references: {retrieval.guide_references}")

    print(f"guide_rag hits: {len(excerpts)}")
    if not excerpts:
        print("未检索到指南片段（route 不在 RAG_ROUTES 或 Milvus 无数据）")
        sys.exit(2)

    for index, excerpt in enumerate(excerpts, start=1):
        print(f"\n--- [{index}] ---")
        print(excerpt[:500])
        if len(excerpt) > 500:
            print("...")


if __name__ == "__main__":
    main()
