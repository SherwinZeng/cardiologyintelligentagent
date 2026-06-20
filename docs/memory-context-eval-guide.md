# 结构化记忆 · 上下文构建 · 对话策略 · 评测集 — 实施指南

> 面向 **铭铭 / ai-agent** 的工程化说明（与当前代码对齐，2026-06）。  
> 原则：**稳定事实确定性写入**；**图的边纯规则**；**策略层规则选模式、LLM 只生成**；**改代码先跑评测**。

---

## 0. 当前状态一览

| 能力 | 状态 | 说明 |
|------|------|------|
| PostgreSQL checkpoint（`uid:session`） | ✅ | messages + 全量 `CardiologyState` 跨轮持久化 |
| `clinical_dispatch_node` 每轮 memory commit | ✅ | `MemoryExtractor.commit` → route → policy → `context_bundle` |
| `structured_memory` + `conversation_memory` | ✅ | 称呼、年龄、慢病事实；带 `value/confidence/evidence` |
| `dialogue_policy` 策略层 | ✅ | `identity_recall` / `emergency_red` / `er_doubt_after_high_risk` 等 |
| `ContextBuilder` → LLM system 注入 | ✅ | `invoke_llm_json` 追加 `as_system_prompt(state)` |
| 身份回忆直答（不调 LLM） | ✅ | `greeting` + `build_identity_recall_output` |
| 纯规则 `resolve_route` + sticky | ✅ | `routing/rules.py` + `keywords/` |
| `eval/` rules 回归（约 13 条） | ✅ | 目标扩到 30～50 |
| **性别 `sex` 抽取** | 📋 | `ContextBuilder` 预留读路径，`MemoryExtractor` **尚未写入** |
| **过敏 / 长期用药** | 📋 | schema 规划中有，extractor 未实现 |
| **跨 session 用户级 profile** | 📋 | 等 Java 用户表或 `uid:profile` thread |
| Milvus RAG | ✅ | `clinical_dispatch_node` 检索；`guideReferences` 持久化 + 前端展示 |

---

## 1. 结构化记忆系统

### 1.1 两层存储（边界）

```text
┌─────────────────────────────────────────────────────────┐
│ 用户级 profile（跨 session，跟 uid）                     │
│  display_name, age, sex, chronic_conditions,            │
│  allergies, long_term_meds                              │
│  存：Java 用户表 / Redis uid:profile 或 checkpoint       │
│       thread_id = uid:profile（单独 thread）             │
└─────────────────────────────────────────────────────────┘
                          ↓ 每次会话注入上下文
┌─────────────────────────────────────────────────────────┐
│ 会话级 episode（本 session，跟 uid:session）              │
│  route, chief_complaint, hpi_*, triage_level,           │
│  red_flag_suspected, investigation_*                    │
│  存：现有 checkpoint thread_id = uid:session            │
└─────────────────────────────────────────────────────────┘
```

**当前实现：会话级为主**——`structured_memory` / PMH 布尔 / HPI 字段随 `uid:session` checkpoint 保留；跨 session 画像待 Java 侧 formal 用户表后再 Feign 拉取或写回。

### 1.2 记忆字段与写入来源

#### 正式记忆：`structured_memory`

| 路径 | 含义 | 写入方 |
|------|------|--------|
| `profile.display_name` | 称呼 | `MemoryExtractor`（正则） |
| `profile.age` | 年龄 | `MemoryExtractor`（正则） |
| `profile.sex` | 性别 | **未实现**（读路径已预留） |
| `medical_profile.chronic_conditions.{code}` | 慢病事实 | `MemoryExtractor`（关键词 + 否定识别） |

每个 fact 结构：`{ "value": ..., "confidence": "high", "evidence": "用户原话前 80 字" }`。

#### 扁平兼容：`conversation_memory`

| 键 | 来源 |
|----|------|
| `display_name` | 与 `profile.display_name` 同步 |
| `age` | 字符串形式的年龄 |
| `chronic_conditions` | 慢病 code 逗号拼接 |

#### 顶层 state 字段（规则节点直读）

| 字段 | 来源 |
|------|------|
| `user_display_name` | `MemoryExtractor` 写称呼时同步 |
| `hypertension` 等 PMH 布尔 | `MemoryExtractor` 慢病命中；`history` 节点 `_merge_pmh_flags` 补充 |
| `chief_complaint` 等 HPI | 主要在 `symptom` 节点；episode 摘要可读这些字段 |

**用户画像 `user_profile_memory` 与存储的对应关系：**

| `user_profile_memory` 键 | 怎么读（`ContextBuilder.build`） | 怎么写 |
|--------------------------|-----------------------------------|--------|
| `display_name` | `profile.display_name` → `_fact_value`；回退 `user_display_name` | `MemoryExtractor` |
| `age` | `profile.age` → `_fact_value` | `MemoryExtractor` |
| `chronic_conditions` | **不读 structured_memory**，而是 `_chronic_conditions(state)` 把顶层 PMH 布尔转成中文标签 | `MemoryExtractor` + `history` 节点 |

> 注意：`as_system_prompt` 里仍有「性别」行，但当前 `user_profile_memory` **未包含 `sex` 字段**，且 extractor 未写 `profile.sex`——画像性别目前为空。

### 1.3 Memory Commit（确定性，不调 LLM）

**位置**：无独立 `memory_commit_node`；在 `clinical_dispatch_node` 第一步调用。

```text
用户本轮 HumanMessage
    → clinical_dispatch_node（routing/dispatch.py）
        → MemoryExtractor.commit(state, text)
        → resolve_route(decision_state)
        → DialoguePolicy.resolve(decision_state, route, text)
        → 若 policy == identity_recall → route 强制改为 greeting
        → ContextBuilder.build → context_bundle
    → merge 进 state（structured_memory / dialogue_policy / context_bundle …）
    → conditional_edges → symptom / lab / greeting … 业务节点
```

**核心实现：**

```text
cardiology_chat/graph/dialogue_core.py
├── MemoryExtractor      # 结构化记忆写入
├── DialoguePolicy       # 本轮回复策略
├── ContextBuilder       # 画像 + 症状事件 + 最近对话
└── build_identity_recall_output  # 身份回忆直答

cardiology_chat/memory/
├── extractors.py      # 练习用小函数，内部调 MemoryExtractor
└── commit.py            # commit_user_facts → MemoryExtractor.commit
```

**姓名正则（当前代码，比早期文档更宽）：**

```python
_NAME_RE = re.compile(
    r"(?:我叫|我的名字是|我的名字叫|叫我|你可以叫我|称呼我为|称呼我|我是)"
    r"([^\s，,。！!？?、；;：:]{1,12})"
)
```

**禁止**：让 Symptom LLM 在回复里「猜」用户叫什么；回忆类问题 **只读 memory**，没有就诚实说不知道。

### 1.4 checklist

- [x] `MemoryExtractor`：`display_name`、`age`、高置信慢病 → `structured_memory` + PMH 布尔
- [ ] `MemoryExtractor`：`sex`（男/女/男性/女性，注意否定）
- [ ] `allergies`、`long_term_meds`
- [x] `clinical_dispatch_node` 每轮 commit → route → policy → context
- [x] 单测：`tests/test_memory_extractors.py`
- [x] `identity_recall`：`greeting` + `build_identity_recall_output`，不调 LLM

---

## 2. 上下文构建器（Context Builder）

### 2.1 发给模型前的结构

```text
[System]
  A. 角色与 JSON 输出格式（各 prompts/llm/*_llm.py）
  B. 【结构化上下文】（ContextBuilder.as_system_prompt）
      · 用户画像：称呼 / 年龄 / （性别预留）/ 基础病
      · 当前症状事件：主诉、性质、起病、缓解、分诊、红旗
      · 本轮对话策略：dialogue_policy 名
  C. 硬约束：优先遵守结构化上下文，不与已知事实冲突

[Messages]
  D. conversation_messages_for_llm：最近 12 条（user 全文，assistant 截断 480 字）
  E. 当前用户问题（最新 HumanMessage）
```

`context_bundle` 内还有 `recent_dialogue`（最近 8 条，user 400 / assistant 240 字），供后续扩展；**当前 LLM 主路径仍走 `messages.py` 窗口**。

### 2.2 实现位置

```text
cardiology_chat/graph/dialogue_core.py → ContextBuilder
cardiology_chat/graph/llm/invoke.py    → invoke_llm_json 追加 as_system_prompt(state)
cardiology_chat/graph/routing/dispatch.py → 每轮写入 state.context_bundle
```

### 2.3 症状事件摘要（episode）

`ContextBuilder._active_symptom_summary`：**优先读 state 结构化字段**，缺省时用全文关键词兜底：

| 字段 | state 字段 | 兜底规则 |
|------|------------|----------|
| 主诉 | `chief_complaint` | — |
| 性质 | `symptom_character` | 全文含「压榨/压迫」→「压榨感/压迫感」 |
| 起病 | `symptom_onset` | 全文含「突然/突发…」→「突然发生」 |
| 缓解 | `relieving_factors` | 全文含「好多了/缓解/减轻」→「部分缓解」 |
| 分诊 | `triage_level` | — |
| 红旗 | `red_flag_suspected` | — |

`chief_complaint` 等主要由 `symptom_collection_node` 写入；dispatch 在每轮业务节点**之前** build context，故首轮主诉可能仍空，靠关键词兜底。

### 2.4 checklist

- [x] `ContextBuilder.build` + `as_system_prompt`
- [x] `invoke_llm_json` 注入结构化上下文
- [ ] 单测：给定 fixture state，断言 system 各块出现/省略
- [ ] `user_profile_memory` 与 `as_system_prompt` 性别字段对齐（补 sex 抽取或去掉 prompt 行）

---

## 3. 对话策略层（Dialogue Policy）

### 3.1 策略 ≠ 路由

| `route`（图边） | `dialogue_policy`（本轮怎么回） |
|-----------------|--------------------------------|
| symptom / lab / … | 同 route 下还可分：高危急诊 / 记名回忆 / 普通过问诊 |

**策略由 `DialoguePolicy.resolve` 规则计算**，不交给 LLM 选。结果写入 `state.dialogue_policy`，并写入 `context_bundle.policy`。

### 3.2 当前策略枚举（代码实际返回值）

```text
identity_recall              # 「我叫什么」等 → dispatch 强制 route=greeting
emergency_red                # 高危语境，非急诊犹豫
er_doubt_after_high_risk     # 高危 + 「一定要去吗」等
symptom_intake
lab_interpret
medication_education
history_intake
small_chat
off_topic_or_fallback
```

### 3.3 当前实现（`DialoguePolicy.resolve`）

```python
if is_identity_recall(text):
    return "identity_recall"
if is_high_risk_context(state):
    return "er_doubt_after_high_risk" if is_er_doubt_question(text) else "emergency_red"
if route == "symptom":
    return "symptom_intake"
# … lab / medication / history / greeting …
```

`is_high_risk_context` 与 symptom 节点对齐：`red_flag_suspected`、`triage_level==red`、全文红旗词、或「突发 + 压榨/压迫」组合。

### 3.4 策略落地方式

| 策略 | 落地 |
|------|------|
| `identity_recall` | `greeting_response_node` → `build_identity_recall_output`，**不调 LLM** |
| `er_doubt_after_high_risk` | `symptom.py` fast path → `ER_DOUBT_RED_FALLBACK` |
| `emergency_red` | symptom 红旗 / 高危保持 red，避免长问卷 |
| 其余 | 写入 system 的「本轮对话策略：xxx」+ 各节点 LLM |

### 3.5 checklist

- [x] `DialoguePolicy.resolve`
- [x] `clinical_dispatch_node` 写入 `dialogue_policy`
- [x] 评测用例断言 `dialogue_policy`（如 `er_doubt_after_high_risk`）

---

## 4. 评测集（30～50 条固定用例）

### 4.1 目录

```text
services/ai-agent/eval/
├── cases/cardiology_regression.json
├── run_eval.py
└── README.md
```

### 4.2 用例 JSON 格式

```json
{
  "id": "er_doubt_after_crushing",
  "tier": "rules",
  "description": "突发压榨感缓解后质疑是否必须急诊",
  "turns": [
    "我心脏不舒服",
    "压榨感",
    "突然间就这样了但现在好多了",
    "一定要去吗"
  ],
  "inject_before_turn": {
    "3": { "triage_level": "red", "chief_complaint": "我心脏不舒服" }
  },
  "assert": {
    "routes": ["symptom", "symptom", "symptom", "symptom"],
    "policies": ["symptom_intake", "symptom_intake", "symptom_intake", "er_doubt_after_high_risk"]
  }
}
```

**tier 说明：**

| tier | 依赖 API | 测什么 |
|------|----------|--------|
| `rules` | 否 | `resolve_route`、`MemoryExtractor`、`DialoguePolicy` |
| `e2e` | 是 | 完整 graph + urgency、文案 must_contain |

### 4.3 建议覆盖矩阵（凑满 30～50 条）

| 类别 | 条数 | 示例 |
|------|------|------|
| 记名写入与回忆 | 5 | `name_intro`, `recall_name_ok`, `recall_name_missing` |
| 症状 HPI 多轮 | 8 | `chest_pain_hpi`, `palpitation_hpi` |
| 高危 + 急诊质疑 | 6 | `er_doubt_after_crushing` |
| 部分缓解不降 red | 3 | `partial_relief_stays_high_risk` |
| sticky 短句 | 5 | `short_reply_stays_symptom` |
| lab 报告 / 科普 | 6 | `ecg_report`, `qrs_education_no_referral` |
| 用药 / 既往史 | 5 | `statin_question`, `pmh_hypertension` |
| 寒暄 / 跑题 | 4 | `pure_hello`, `weather_off_topic` |

### 4.4 怎么跑

```bash
cd services/ai-agent

poetry run python eval/run_eval.py --tier rules
poetry run python -m unittest tests.test_route_rules tests.test_symptom_routing tests.test_memory_extractors -v
poetry run python eval/run_eval.py --tier e2e
```

### 4.5 改代码流程

```text
改 prompt / graph / memory / dialogue_core
    → eval --tier rules（必须先绿）
    → 本地 eval --tier e2e
    → 合并
```

---

## 5. 实施阶段（对照进度）

| Sprint | 内容 | 状态 |
|--------|------|------|
| **S1** | MemoryExtractor + dispatch commit + 记名单测 | ✅ |
| **S2** | ContextBuilder + LLM prompt 注入 | ✅ |
| **S3** | DialoguePolicy + 高危/身份策略 | ✅ |
| **S4** | 评测扩 30～50 + CI 挂 rules | 🚧 |
| **S5** | sex / 过敏 / 用药记忆 + 跨 session profile | 📋 |

---

## 6. 与 LoRA 的关系

| 组件 | LoRA 是否训练 |
|------|----------------|
| MemoryExtractor | 否，永远规则 |
| resolve_route / DialoguePolicy | 否，永远规则 |
| ContextBuilder 格式 | 否，代码固定 |
| 各节点 JSON 回复 | **是** |

微调 JSONL 的 `system` 应包含与 **`ContextBuilder.as_system_prompt` 同结构** 的「用户画像 + 症状事件 + 策略」块，否则线上/offline 行为分裂。见 [lora-finetune.md](./lora-finetune.md)。

---

## 7. 相关文件索引

| 主题 | 路径 |
|------|------|
| 对话核心层 | `cardiology_chat/graph/dialogue_core.py` |
| 逐行阅读注释 | [dialogue-core-line-by-line.md](./dialogue-core-line-by-line.md) |
| State 字段 | `cardiology_chat/graph/state.py` |
| Dispatch 接线 | `cardiology_chat/graph/routing/dispatch.py` |
| 路由规则 | `graph/routing/rules.py` + `graph/routing/keywords/` |
| LLM 调用 | `cardiology_chat/graph/llm/invoke.py` |
| 高危兜底 | `cardiology_chat/prompts/fallback.py` |
| 评测 | `services/ai-agent/eval/run_eval.py` |

**下一步建议**：在 `eval/cases/cardiology_regression.json` 按矩阵补用例；红的 rules 用例即当前最该修的 bug。
