# 铭铭回归评测集

## 快速开始

```bash
cd services/ai-agent

# rules：路由 + 记忆 + dialogue_policy（不需要 API Key）
poetry run python eval/run_eval.py --tier rules

# 单测
poetry run python -m unittest tests.test_route_rules tests.test_symptom_routing tests.test_memory_extractors -v

# 完整多轮 graph（需要 DEEPSEEK_API_KEY）
poetry run python eval/run_eval.py --tier e2e
```

## tier 说明

| tier | 测什么 | 依赖 |
|------|--------|------|
| `rules` | `resolve_route`、sticky、`MemoryExtractor`、`DialoguePolicy.resolve` | 无 API |
| `e2e` | 完整 graph：`urgency`、`must_contain` / `must_not_contain` | `DEEPSEEK_API_KEY` |

`rules` 层通过 `clinical_dispatch_node` 等价路径校验每轮的 `route` 与 `dialogue_policy`（见 `eval/run_eval.py`）。

## 加用例

编辑 `cases/cardiology_regression.json`：

```json
{
  "id": "example",
  "tier": "rules",
  "turns": ["我叫小曾", "我叫什么"],
  "assert": {
    "routes": ["greeting", "greeting"],
    "policies": ["small_chat", "identity_recall"]
  }
}
```

| 字段 | 说明 |
|------|------|
| `turns` | 多轮用户输入 |
| `inject_before_turn` | 模拟 checkpoint 已有字段，如 `triage_level`、`chief_complaint` |
| `assert.routes` | 每轮期望 `route` |
| `assert.policies` | 每轮期望 `dialogue_policy` |
| `assert.must_contain` | e2e：最终回复应包含的子串 |

目标 **30～50 条**，覆盖矩阵见 [memory-context-eval-guide.md](../../docs/memory-context-eval-guide.md)。

## CI 建议

- PR：`eval/run_eval.py --tier rules` + `test_memory_extractors`
- 夜间：`--tier e2e`
