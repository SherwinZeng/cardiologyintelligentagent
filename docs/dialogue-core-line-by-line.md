# `dialogue_core.py` 逐行阅读注释

对应文件：`services/ai-agent/cardiology_chat/graph/dialogue_core.py`（约 266 行，2026-06 与源码对齐）

读法：左边打开源码，右边看本文。先跑通四个问题：

```text
1. 用户这句话有没有稳定事实可以记住？
2. 这轮应该走什么策略（dialogue_policy）？
3. 要给 LLM 看哪些上下文（context_bundle）？
4. 有没有场景可以不调用 LLM 直接回答？
```

---

## 总览

```text
L1-37    import + 规则常量（正则、慢病别名、非姓名黑名单）
L40-91   MemoryExtractor：写 structured_memory / conversation_memory / PMH 布尔
L94-131  DialoguePolicy：决定本轮 dialogue_policy
L134-245 ContextBuilder：构建 context_bundle + as_system_prompt
L248-265 build_identity_recall_output：身份回忆直答
```

接线：`routing/dispatch.py` 每轮调用上述三类 + 写入 `context_bundle`；`llm/invoke.py` 调 LLM 时追加 `as_system_prompt`。

---

## L1-37：依赖与规则常量

- **L17-20 `_NAME_RE`**：匹配「我叫 / 叫我 / 我是 / 称呼我…」+ 捕获名字（最多 12 字，排除标点）。
- **L21 `_AGE_RE`**：匹配「35岁」「我今年42周岁」等。
- **L22-23**：突发起病、压榨/压迫痛关键词，用于高危语境判断。
- **L24-25 `_IDENTITY_RECALL_MARKERS`**：「我叫什么」「怎么称呼我」等——命中后 **不抽姓名**。
- **L26-28 `_NOT_A_NAME_MARKERS`**：黑名单，避免「我是高血压」「我叫什么」误抽。
- **L31-37 `_CONDITION_ALIASES`**：慢病 code → 高置信别名；命中后写 `medical_profile.chronic_conditions` 与顶层布尔。

---

## L40-91：`MemoryExtractor`

### `commit(state, text)` 流程

1. **L43-44**：空文本 → 返回 `{}`，不改 state。
2. **L46-49**：深拷贝 `structured_memory`；确保 `profile`、`medical_profile` 存在；复制 `conversation_memory`。
3. **L51-55**：若是身份回忆问句，跳过姓名正则；否则 `_NAME_RE.search`。
4. **L56-61 姓名**：清洗语气词 → 写 `profile.display_name`（`_fact` 结构）→ `conversation_memory` → `user_display_name`。
5. **L63-68 年龄**：正则取 1–120 岁 → `profile.age` + `conversation_memory.age`。
6. **L70-78 慢病**：遍历 `_CONDITION_ALIASES`，`has_keyword(..., negation_aware=True)` → 写 `chronic_conditions[code]` 与 `updates[code]=True`。
7. **L79-82**：扁平记忆或 structured 有变化才放入 `updates`（避免无意义 merge）。
8. **L83**：返回 `updates` 供 LangGraph merge。

### 小工具

- **`_clean_name`**：去掉「呀啊呢的」等尾巴。
- **`_fact(value, evidence)`**：`{value, confidence: high, evidence[:80]}`。

### 当前未实现

- **`profile.sex`**：未在 `commit` 中写入（`ContextBuilder` 读路径预留，`as_system_prompt` 有性别行但数据为空）。
- **过敏、长期用药**：未实现。

---

## L94-131：`DialoguePolicy`

- **`resolve(state, route, text)`** 优先级：
  1. `identity_recall`（身份回忆问句）
  2. 高危语境 → `er_doubt_after_high_risk`（`is_er_doubt_question`）或 `emergency_red`
  3. 按 `route` 映射：`symptom_intake` / `lab_interpret` / … / `small_chat`
  4. 兜底 `off_topic_or_fallback`
- **`is_high_risk_context`**：`red_flag` / `triage red` / 全文红旗词 / （突发 + 压榨）组合。
- dispatch 里若 policy 为 `identity_recall`，会把 **route 强制改为 `greeting`**。

---

## L134-245：`ContextBuilder`

### `build(state, route, policy)`

从 state 组装 `context_bundle`：

| 输出键 | 内容 |
|--------|------|
| `policy` | 本轮 `dialogue_policy` |
| `route` | 图分支 |
| `user_profile_memory` | 见下表 |
| `active_symptom_summary` | HPI + 分诊 + 红旗（含关键词兜底） |
| `recent_dialogue` | 最近 8 条消息（user 400 / assistant 240 字） |
| `current_user_message` | 最新用户句 |

**`user_profile_memory` 字段来源：**

| 键 | 读取 |
|----|------|
| `display_name` | `_fact_value(profile.display_name)` 或 `user_display_name` |
| `age` | `_fact_value(profile.age)` |
| `chronic_conditions` | `_chronic_conditions(state)`：读顶层 PMH 布尔 → 中文标签列表 |

> 当前 **`user_profile_memory` 不含 `sex`**；`as_system_prompt` 里 `profile.get("sex")` 行通常不会输出。

### `as_system_prompt(state)`

- 优先用 state 里 dispatch 已写的 `context_bundle`。
- 拼「【结构化上下文】」：用户画像、当前症状事件、本轮策略、硬约束句。
- 由 `invoke_llm_json` 追加到各节点 system prompt 末尾。

### `_active_symptom_summary`

- 优先 `state.chief_complaint` 等结构化字段。
- 缺省时用 `all_user_text(state)` + 关键词推断性质/起病/缓解。

### `_recent_dialogue`

- 默认 8 条；与 `messages.py` 的 12 条 LLM 窗口是 **两套机制**（bundle 供扩展，主 LLM 路径仍用 `conversation_messages_for_llm`）。

---

## L248-265：`build_identity_recall_output`

- 读 `structured_memory.profile.display_name` → 回退 `user_display_name` → `conversation_memory.display_name`。
- 有名字：确定性回答「你叫 xxx」。
- 无名字：诚实说不知道，引导用户说「我叫…」。
- 返回 Java 四件套映射字段；`triage_level=green`；**不调 LLM**。

---

## 练手链路

```text
用户：你好我叫小曾
  → MemoryExtractor.commit 写 display_name / user_display_name
  → DialoguePolicy → small_chat（route 多为 greeting）
  → ContextBuilder → user_profile_memory.display_name=小曾

用户：我叫什么
  → MemoryExtractor 不抽新名字（身份回忆 marker）
  → DialoguePolicy → identity_recall；dispatch 改 route=greeting
  → greeting_response_node → build_identity_recall_output
  → 不调 LLM，直接回答「小曾」
```

对应测试：`services/ai-agent/tests/test_memory_extractors.py`（含 `test_graph_recalls_name_without_llm`）。

---

## 与 `memory/` 包的关系

```text
memory/extractors.py  → 薄封装，内部调 MemoryExtractor.commit
memory/commit.py      → commit_user_facts → MemoryExtractor.commit
```

正式逻辑 **只在 `dialogue_core.py`**；`memory/` 保留给单测和练习。
