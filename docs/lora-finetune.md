# LoRA 微调计划（Cardiology 铭铭）

> **当前阶段（2026-06）**：多轮对话已用 **PostgreSQL checkpointer** 收口；**图路由为纯规则**（不调 Router LLM）；下一步对 **节点内对话 LLM** 做 LoRA 微调，与云端 DeepSeek Flash 对比后决定是否接入生产。

---

## 1. 目标

| 目标 | 说明 |
|------|------|
| **领域对齐** | 心血管问诊语气、分诊表述、免责声明、拒答边界 |
| **降本 / 可控** | 高频 `general-understanding` 可切换本地 LoRA 推理 |
| **可对比** | 同一 LangGraph 图 + 同一 checkpoint，只换 LLM 后端 |

**不指望 LoRA 单独解决多轮记忆**——跨轮仍靠 **PostgreSQL checkpointer**；微调补的是 **单轮生成质量** 与 **JSON 稳定性**。

---

## 2. 与现有架构的关系

```text
前端 → Java session（存消息给 UI）
         ↓ Feign { uid, session, message }
ai-agent → Postgres checkpointer（thread_id = uid:session）
         → LangGraph（resolve_route 纯规则 → 各业务节点 LLM）
         → LLM 后端 ← 【LoRA 微调接入点】
              · 现网：DeepSeek V4 Flash（API）
              · 实验：本地 vLLM / Ollama + LoRA adapter
```

| 组件 | 是否微调 | 备注 |
|------|----------|------|
| LangGraph 图结构 | 否 | 节点/Prompt 仍可迭代 |
| `routing/rules.py` | 否 | **纯规则**选边；sticky + 默认 symptom；不微调 |
| Checkpointer | 否 | 只存 state |
| **各节点对话 LLM** | **主战场** | greeting / symptom / lab / medication 等 JSON 四件套 |
| ~~Router LLM~~ | — | 已停用，LoRA **不**用于图路由 |

---

## 3. 基座模型（建议起步）

| 方案 | 基座 | 说明 |
|------|------|------|
| A（推荐） | Qwen2.5-7B-Instruct | 中文好、LoRA 生态成熟、单卡可训 |
| B | 同尺寸开源 Instruct | 需自行验证授权与效果 |

训练框架：**LLaMA-Factory** / **Unsloth** / **PEFT + trl**。

---

## 4. 数据

### 来源

1. 脱敏 `chat_message` 导出 JSONL
2. 按 `services/ai-agent/cardiology_chat/prompts/llm/*_llm.py` 人工补场景
3. 多轮样本（user-assistant 交替，≥3 轮）

### 格式（SFT）

```json
{
  "messages": [
    {"role": "system", "content": "<与 symptom_llm 等一致的 system>"},
    {"role": "user", "content": "最近胸口闷"},
    {"role": "assistant", "content": "{\"urgency\":\"yellow\",\"explanation\":\"...\",\"advice\":\"...\",\"disclaimer\":\"...\"}"}
  ]
}
```

assistant 必须与线上一致：**严格 JSON**，字段 `urgency/explanation/advice/disclaimer`。

### 首版规模

约 **2k～4k** 条（symptom 为主 + lab/medication/history/greeting + 多轮 200+）。

---

## 5. LoRA 超参（参考）

```yaml
r: 16
lora_alpha: 32
lora_dropout: 0.05
target_modules: [q_proj, k_proj, v_proj, o_proj, gate_proj, up_proj, down_proj]
learning_rate: 1e-4
num_train_epochs: 3
max_seq_length: 4096
```

7B + LoRA：约 **1×24GB** 可训；显存紧张用 QLoRA。

---

## 6. 接入 ai-agent（计划）

| 文件 | 改动 |
|------|------|
| `configuration/settings.py` | `LLM_BACKEND=deepseek\|local`，`LOCAL_LLM_BASE_URL` |
| `cardiology_chat/factory/LLMFactory.py` | OpenAI-compatible 本地端点 |

**不改** checkpointer、Feign 契约、`chat_graph_service.py`。

---

## 7. 评估

- 离线：JSON 可解析率、必填字段非空、人工安全/语气打分
- 在线：同一 LangGraph + checkpoint，对比 Flash vs LoRA（延迟、成本、质量）

---

## 8. 任务清单

### Phase 1 · 数据（1～2 周）

- [ ] 定基座 + 训练框架
- [ ] `scripts/export_chat_messages.py`（脱敏 JSONL）
- [ ] 首版 train/val 9:1，≥2000 条

### Phase 2 · 训练（约 1 周）

- [ ] LoRA SFT → adapter `v1`
- [ ] JSON 解析率 ≥95%

### Phase 3 · 接入 A/B（约 1 周）

- [ ] `LLMFactory` 支持 local
- [ ] vLLM/Ollama 部署 + 对比报告

---

## 9. 相关文档

- [beta2-plan.md](./beta2-plan.md)
- [services/ai-agent/README.md](../services/ai-agent/README.md)
