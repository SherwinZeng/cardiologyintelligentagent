<div align="center">

# 🌸 Cardiology Intelligent Agent

**心血管智能问诊 Agent · 铭铭**

[![Python](https://img.shields.io/badge/Python-3.13+-3776AB?logo=python&logoColor=white)](https://www.python.org/)
[![Django](https://img.shields.io/badge/Django-6.0-092E20?logo=django&logoColor=white)](https://www.djangoproject.com/)
[![DRF](https://img.shields.io/badge/DRF-3.17-red)](https://www.django-rest-framework.org/)
[![LangChain](https://img.shields.io/badge/LangChain-1.3-1C3C3C?logo=langchain&logoColor=white)](https://www.langchain.com/)
[![LangGraph](https://img.shields.io/badge/LangGraph-1.2-1C3C3C)](https://langchain-ai.github.io/langgraph/)
[![DeepSeek](https://img.shields.io/badge/DeepSeek-V4_Flash-1C3C3C)](https://www.deepseek.com/)
[![Qwen](https://img.shields.io/badge/Qwen-3.7_Plus-615EFF)](https://tongyi.aliyun.com/)
[![Poetry](https://img.shields.io/badge/Poetry-依赖管理-60A5FA?logo=poetry&logoColor=white)](https://python-poetry.org/)
[![Redis](https://img.shields.io/badge/Redis-6+-DC382D?logo=redis&logoColor=white)](https://redis.io/)

`services/ai-agent/`

[简介](#简介) · [LangGraph](#langgraph-工作流) · [多轮 LLM 统一架构](#多轮-llm-统一架构) · [graph 目录](#graph-目录导读) · [维护指南](#维护指南) · [启动](#快速开始) · [API](#api-文档)

</div>

---

## 简介

**ai-agent** 是心血管问诊系统的 Python AI 服务，核心角色是 **铭铭** —— 基于 LangGraph 的心血管健康咨询 Agent。

**主要能力：**

- 症状采集与分诊（`green` / `yellow` / `red`）
- **用药咨询**（CCB / ARB 等，独立 `medication` 节点）
- 既往史与危险因素询问
- 检查 / 化验报告解读
- 寒暄、自我介绍
- 非心血管话题拒答
- 多轮对话（PostgreSQL checkpointer；Java 不传 `history`）
- 结构化 JSON 输出

> ⚠️ 仅供健康信息参考，不能替代医生诊断与处方。

---

## 功能状态

| 接口 | 路由 | 模型 | 状态 |
|------|------|------|------|
| 普通问诊 | `POST /general-understanding/` | LangGraph + **DeepSeek V4 Flash** | ✅ |
| 深度推理 | `POST /reasoning/` | **DeepSeek Pro** | 📋 |
| 多模态 | `POST /multimodal/` | **通义千问 Qwen 3.7 Plus** | 📋 |

### 多模态覆盖（规划 · Qwen）

| 模态 | 示例输入 | 说明 |
|------|----------|------|
| ECG | 静态心电图、截图 | 心律、ST 段等辅助解读 |
| Holter | 动态心电图报告 | 24h 监测摘要 |
| CTA | 冠脉 CT、冠脉 CTA | 钙化积分、狭窄描述辅助 |
| 心脏彩超 | 超声报告 / 图像 | 结构、EF 等指标摘要 |
| 化验 / 报告 | PDF、检验单照片 | 与 `lab` 节点联动 |

---

## 模型路由

```mermaid
flowchart LR
    REQ["Java Feign 请求"]
    ROUTE{"路由"}
    LG["LangGraph<br/>general-understanding"]
    REASON["reasoning"]
    MULTI["multimodal"]
    DS_FLASH["DeepSeek V4 Flash ✅"]
    DS_PRO["DeepSeek Pro 📋"]
    QW["Qwen 3.7 Plus 📋"]

    REQ --> ROUTE
    ROUTE -->|文本问诊| LG --> DS_FLASH
    ROUTE -->|深度推理| REASON --> DS_PRO
    ROUTE -->|影像/PDF| MULTI --> QW

    style DS_PRO fill:#fff,stroke-dasharray: 5 5
    style QW fill:#fff,stroke-dasharray: 5 5
    style MULTI fill:#fff,stroke-dasharray: 5 5
    style REASON fill:#fff,stroke-dasharray: 5 5
```

---

## 技术栈

| 类别 | 技术 |
|------|------|
| Web | Django 6.0 · DRF |
| Agent | LangGraph · LangChain |
| LLM | **DeepSeek V4 Flash**（文本）· **DeepSeek Pro**（推理，规划）· **通义千问 Qwen 3.7 Plus**（多模态，规划） |
| 多轮记忆 | **PostgreSQL checkpointer**（`PostgresSaver`，`thread_id=uid:session`）；Java 消息库仅用于前端展示 |
| 鉴权 | Redis 内部 token |
| 包管理 | Poetry |

---

## 新手读代码（5 分钟）

**一轮用户消息怎么走：**

```text
views.py  POST general-understanding/
    → chat_graph_service.invoke_general_understanding()
        → graph.invoke（只 append 本轮 HumanMessage）
        → START → clinical_dispatch_node
            → MemoryExtractor.commit（称呼/年龄/慢病）
            → resolve_route + DialoguePolicy.resolve
            → ContextBuilder.build → context_bundle
        → symptom / lab / greeting … 某一个节点
        → END
        → update_state 写入 AIMessage（铭铭回复）
    → 返回 urgency / explanation / advice / disclaimer
```

| 想搞懂… | 先看文件 |
|---------|----------|
| 图有几条边、几个节点 | `graph/builder.py` |
| 每轮怎么 invoke | `services/chat_graph_service.py` |
| 记忆 / 画像 / 策略 / 上下文 | `graph/dialogue_core.py` + `graph/routing/dispatch.py` |
| 选 symptom 还是 lab | `graph/routing/rules.py` + `graph/routing/keywords/` |
| 胸痛怎么回、急诊逻辑 | `graph/nodes/intake/symptom.py` |
| 调 LLM、JSON 四件套 | `graph/llm/invoke.py` + `graph/llm/conversation_node.py` |
| 多轮存在哪 | `graph/checkpointer.py`（Postgres，`uid:session`） |

**分工记住两句：**

- **图的边（route）** → 纯规则，不调 LLM；LangChain **没有**名为「IRM 意图识别」的独立模块，官方是 Router + `with_structured_output` **套路**，本项目已选用 **规则 `resolve_route()`**。
- **节点里说什么** → DeepSeek Flash + 各 `prompts/llm/*_llm.py`；红旗 / 急诊犹豫等安全逻辑用 **规则 fast path**。

更细的工程计划见 [docs/memory-context-eval-guide.md](../../docs/memory-context-eval-guide.md)。

---

## LangGraph 工作流

每轮用户消息的执行路径：

```mermaid
flowchart TD
    startPoint(["START"]) --> dispatch["clinical_dispatch_node<br/>routing/dispatch.py<br/>rules.py + keywords/"]

    dispatch -->|symptom| n_symptom["symptom_collection_node<br/>症状采集 · intake/symptom.py<br/>追问现病史 + 分诊<br/>红旗词 → 直接 red"]
    dispatch -->|history| n_history["medical_history_inquiry_node<br/>既往史采集 · intake/history.py<br/>提取 PMH 标记 + LLM 追问"]
    dispatch -->|medication| n_medication["medication_consultation_node<br/>用药咨询 · intake/medication.py<br/>CCB/ARB/他汀科普，不给剂量"]
    dispatch -->|greeting| n_greeting["greeting_response_node<br/>寒暄 · response/greeting.py<br/>你好/作者/曾祥瑞彩蛋"]
    dispatch -->|fallback| n_fallback["medical_fallback_response_node<br/>宽口径兜底 · response/fallback.py<br/>规则判无关时仍可能答心血管"]
    dispatch -->|lab| n_lab["lab_report_interpret_node<br/>检查解读 · diagnostics/lab.py"]

    n_lab -->|needs_risk_referral| n_risk["cardiac_risk_stratification_node<br/>风险评估 · diagnostics/risk.py<br/>规则汇总 PMH + 检查，不调 LLM"]
    n_lab -->|科普/怎么看| graphEnd(("END"))
    n_risk --> n_referral["physician_referral_node<br/>转诊建议 · diagnostics/referral.py<br/>按 triage 拼接科室与急诊提示"]

    n_symptom --> graphEnd(("END"))
    n_history --> graphEnd
    n_medication --> graphEnd
    n_greeting --> graphEnd
    n_fallback --> graphEnd
    n_referral --> graphEnd
```

> **说明：** `lab` 有具体报告/异常时走 risk → referral；科普「怎么看」类单节点结束。其余 route 单节点后直接 END。

### 多轮 LLM 统一架构

所有多轮节点共用同一套管道，避免 greeting / symptom / fallback 各写各的：

```text
checkpoint 恢复 messages + state + 本轮 message
        ↓
clinical_dispatch_node
    MemoryExtractor.commit → resolve_route → DialoguePolicy → ContextBuilder
        ↓
业务节点（invoke_llm_json_with_retry 等）
        ↓
写回 checkpoint（messages + structured_memory / dialogue_policy / context_bundle …）
```

LLM 读上下文时：

- **System 追加**：`ContextBuilder.as_system_prompt(state)`（用户画像 + 症状事件 + 本轮策略）
- **Messages 窗口**：`conversation_messages_for_llm` 仍只取 **最近 12 条**，assistant 截断 **480 字**；checkpoint 内可存更长历史

```text
prompts/llm/shared.py     →  CONVERSATION_RULES（回忆/不知道/不猜/不贴记录，只写一次）
        ↓
invoke_llm_json_with_retry →  temperature 0.3 → 0.1，失败返回 {}
        ↓
resolve_static_impression  →  LLM 全挂时按 route 选静态兜底（不猜名字）
```

| 模块 | 文件 | 职责 |
|------|------|------|
| 对话规则 | `prompts/llm/shared.py` | `with_conversation_rules()` 追加多轮规则 |
| LLM 调用 | `graph/llm/invoke.py` | `invoke_llm_json` / `invoke_llm_json_with_retry` |
| 多轮节点入口 | `graph/llm/conversation_node.py` | `run_standard_conversation_node()` |
| 静态兜底 | `prompts/fallback.py` | `resolve_static_impression(state, route)`；高危 symptom 不走长问卷 |
| 消息窗口 | `graph/llm/messages.py` | user 原文；assistant 截断 480 字再送 LLM |

**回忆类问题约定（LLM + 静态兜底一致）：**

- 上文有明确信息且确定在问本人 → 直接回答
- 没有 / 不确定 / 在问别人 → **诚实说不知道**，请用户补充
- 禁止猜测、禁止罗列聊天记录、禁止无关的通用自我介绍

> `lab` 分支由 LLM 字段 `needs_risk_referral` 决定：有具体报告/异常时走 risk → referral，科普「怎么看」类单节点结束。

### 节点注释（逐节点说明）

#### 1. `clinical_dispatch_node` — 记忆 + 路由 + 策略 + 上下文

| 项 | 说明 |
|----|------|
| **文件** | `graph/routing/dispatch.py` · `graph/dialogue_core.py` · `graph/routing/rules.py` |
| **触发** | 每轮入口（START 后第一个节点） |
| **输入** | checkpoint 恢复的 `state`（`messages`、`structured_memory`、`chief_complaint`、`triage_level` 等） |
| **逻辑** | ① 空消息 → `fallback` ② **`MemoryExtractor.commit`**：从本轮用户句规则抽取称呼、年龄、慢病 → `structured_memory` + PMH 布尔 ③ **`resolve_route`**：关键词 + sticky，默认 `symptom` ④ **`DialoguePolicy.resolve`** → `dialogue_policy`；`identity_recall` 时强制 `route=greeting` ⑤ **`ContextBuilder.build`** → `context_bundle`。**不调 Router LLM** |
| **输出** | `route`、`dialogue_policy`、`context_bundle`、`structured_memory` 等 merge 字段 |
| **改哪里** | 画像抽取 → `dialogue_core.py`；路由 → `keywords/` + `rules.py`；策略 → `DialoguePolicy`；`tests/test_memory_extractors.py` |

#### 2. `symptom_collection_node` — 症状采集

| 项 | 说明 |
|----|------|
| **文件** | `graph/nodes/intake/symptom.py` |
| **适用** | 用户描述胸痛、心悸、气短等现病史 |
| **逻辑** | ① **部分缓解**（如「现在好多了」）≠ 症状结束，不清 red 锁 ② 用户明确说「完全好了」等 → LLM，可解除红旗 ③ **本轮**出现红旗词 → 固定 red 模板 ④ 高危语境下「一定要去吗」等急诊犹豫问题 → `ER_DOUBT_RED_FALLBACK` fast path，不调 LLM，不贴首轮 16 题问卷 ⑤ 否则 `run_standard_conversation_node`（LLM + 静态兜底）⑥ 曾 red / 高危语境 → 分诊至少 red |
| **输出** | `chief_complaint`、`triage_level`、`clinical_impression`、`management_advice`、`red_flag_suspected` |
| **改哪里** | 红旗词 → `prompts/symptom.py`；回答风格 → `prompts/llm/symptom_llm.py`；多轮规则 → `prompts/llm/shared.py` |

#### 3. `medical_history_inquiry_node` — 既往史采集

| 项 | 说明 |
|----|------|
| **文件** | `graph/nodes/intake/history.py` |
| **适用** | 用户聊高血压、糖尿病、冠心病史、吸烟、家族史等 |
| **逻辑** | 从全对话文本规则提取 PMH 布尔标记（高血压、糖尿病…）并 merge 到 state；高风险组合 → 上调 yellow；LLM 生成追问与回复 |
| **输出** | PMH 字段、`family_premature_cad`、`pmh_complete`、`triage_level`、回复三件套 |
| **改哪里** | 词表/组合规则 → `prompts/history.py`；LLM → `prompts/llm/history_llm.py` |

#### 4. `medication_consultation_node` — 用药咨询

| 项 | 说明 |
|----|------|
| **文件** | `graph/nodes/intake/medication.py` |
| **适用** | CCB、ARB、他汀、阿司匹林等用药疑问 |
| **逻辑** | 纯 LLM 节点：`run_standard_conversation_node`；科普机制、注意事项、何时复诊；**不推荐具体剂量** |
| **输出** | `triage_level`（通常 green）、`clinical_impression`、`management_advice`、`medical_disclaimer` |
| **改哪里** | `prompts/llm/medication_llm.py` |

#### 5. `greeting_response_node` — 寒暄与元对话

| 项 | 说明 |
|----|------|
| **文件** | `graph/nodes/response/greeting.py` |
| **适用** | 你好、你是谁、作者是谁、曾祥瑞彩蛋；**「我叫什么」→ `dialogue_policy=identity_recall`** |
| **逻辑** | `identity_recall` → **`build_identity_recall_output`** 直读 `structured_memory`，**不调 LLM**；其余 → `run_standard_conversation_node` + 静态兜底 |
| **输出** | 通常 `triage_level=green`，友好介绍 + 引导回心血管话题 |
| **改哪里** | 身份回忆 → `graph/dialogue_core.py`；寒暄风格 → `prompts/llm/greeting_llm.py` |

#### 6. `medical_fallback_response_node` — 宽口径兜底

| 项 | 说明 |
|----|------|
| **文件** | `graph/nodes/response/fallback.py` |
| **适用** | 规则认为明显非心血管时进入 |
| **逻辑** | `invoke_llm_json_with_retry` + 宽口径 Prompt；`is_off_topic=true` → 温和拒答；否则仍尝试答心血管 |
| **输出** | `triage_level=green` + 拒答或宽口径回答 |
| **改哪里** | `prompts/llm/fallback_llm.py`；静态兜底 → `prompts/fallback.py` |

#### 7. `lab_report_interpret_node` — 检查/化验解读（流水线 ①）

| 项 | 说明 |
|----|------|
| **文件** | `graph/nodes/diagnostics/lab.py` |
| **适用** | 化验单、心电图、超声、CTA 报告；术语科普（QRS、ST 段等） |
| **逻辑** | ① 全对话含危急关键词 → 固定 red ② 否则 LLM 解读，写入 `investigation_summary`；LLM 返回 `needs_risk_referral=false`（科普/无具体报告）→ 单节点结束，不跑 risk/referral |
| **输出** | `investigation_text`、`investigation_summary`、`lab_followup_needed`、分诊与回复 |
| **改哪里** | 危急词 → `prompts/lab.py`；解读 Prompt → `prompts/llm/lab_llm.py`；条件边 → `graph/builder.py` |

#### 8. `cardiac_risk_stratification_node` — 风险评估（流水线 ②）

| 项 | 说明 |
|----|------|
| **文件** | `graph/nodes/diagnostics/risk.py` |
| **适用** | 仅 `lab` 路径且 `lab_followup_needed=true` 时执行 |
| **逻辑** | **不调 LLM**；汇总 PMH 标记 + `investigation_summary` + `red_flag_suspected`，规则评估风险等级并可能上调 `triage_level` |
| **输出** | 追加风险摘要到 `clinical_impression`，追加分层建议到 `management_advice` |
| **改哪里** | `prompts/risk.py`（模板与分层文案） |

#### 9. `physician_referral_node` — 转诊建议（流水线 ③）

| 项 | 说明 |
|----|------|
| **文件** | `graph/nodes/diagnostics/referral.py` |
| **适用** | 仅 `lab` 路径且 `lab_followup_needed=true` 时最后一步 |
| **逻辑** | **不调 LLM**；按 `triage_level` 拼接推荐科室、 urgency、何时去急诊 |
| **输出** | 追加转诊文案到 `management_advice` |
| **改哪里** | `prompts/referral.py` |

### 意图路由（纯规则）

图的 **conditional edge 不由 LLM 判断**。关键词表在 `routing/keywords/`，优先级逻辑在 `routing/rules.py` 的 `resolve_route()`：

| 层级 | 机制 | 说明 |
|------|------|------|
| 1 | **关键词意图** | `keywords/symptom.py` · `lab.py` · `greeting.py` 等 |
| 2 | **会话语境** | `chief_complaint`、`triage_level`、`red_flag_suspected`、全 thread 用户文本 |
| 3 | **sticky route** | 第 2 轮起保持上一轮 `symptom|lab|medication|history` |
| 4 | **宽口径默认** | 其余 → `symptom` |
| 5 | **安全红旗** | `nodes/intake/symptom.py` · `diagnostics/lab.py`（不是路由边） |

```text
routing/keywords/
├── greeting.py      # 寒暄 / 自我介绍 / 作者彩蛋 marker
├── symptom.py       # 症状词 + 多轮追问 marker（「一定要去吗」等）
├── lab.py           # 检查 / 报告词
├── medication.py
├── history.py       # 路由用既往史词（与 history 节点 PMH 提取不同）
└── off_topic.py
```

> `prompts/llm/router_llm.py` 已 **停用**。若改用 LangChain 文档里的 LLM 路由，应单独加 `classify` 节点 + `with_structured_output`，**不要**与当前规则路由混用。

**AI 只负责节点内生成回复**（追问、分诊文案、报告解读 JSON 等），不负责选边。

---

## graph 目录导读

建议阅读顺序（每个文件开头有中文模块注释）：

1. `graph/__init__.py` — 包入口，导出 `cardiology_graph`
2. `graph/builder.py` — 注册节点与边，`compile()` 成可执行图
3. `graph/state.py` — `CardiologyState` 全部字段及与 Java 返回映射
4. `graph/dialogue_core.py` — **MemoryExtractor / DialoguePolicy / ContextBuilder**
5. `graph/routing/dispatch.py` — 每轮第一步：记忆 + 路由 + 策略 + context_bundle
6. `graph/nodes/` — 业务节点（见下表）
7. `graph/llm/` — 公共工具（读消息、调 Flash、解析 JSON、注入结构化上下文）

```text
cardiology_chat/graph/
├── __init__.py              # from cardiology_chat.graph import cardiology_graph
├── builder.py               # 图编排
├── state.py                 # CardiologyState + empty_cardiology_state()
├── dialogue_core.py         # 记忆抽取 / 对话策略 / 上下文构建（核心）
├── utils.py                 # 兼容层 → 转发到 graph.llm
│
├── llm/                     # 节点共用工具
│   ├── messages.py          # 构建 LLM 消息窗口（assistant 截断 480 字）
│   ├── conversation_node.py # run_standard_conversation_node
│   ├── keywords.py          # has_keyword（红旗、危急值等确定性规则）
│   ├── json_parser.py       # 解析 LLM 返回的 JSON
│   └── invoke.py            # invoke_llm_json + ContextBuilder.as_system_prompt
│
├── routing/                 # 意图路由（纯规则，无 LLM）
│   ├── dispatch.py          # clinical_dispatch_node（接线 dialogue_core）
│   ├── rules.py             # resolve_route() 优先级链
│   └── keywords/            # 各场景关键词表
│
├── memory/                  # 兼容入口（内部转发 MemoryExtractor）
│   ├── extractors.py
│   └── commit.py
│
└── nodes/
    ├── intake/              # 信息采集
    │   ├── symptom.py       # 症状 + 红旗（RED_FLAG_KEYWORDS）
    │   ├── history.py       # 既往史 + PMH 布尔标记提取
    │   └── medication.py    # 用药咨询
    ├── diagnostics/         # 检查解读（lab 路由；条件串联 risk/referral）
    │   ├── lab.py           # 报告/术语解读；needs_risk_referral 控制后续
    │   ├── risk.py          # 规则风险评估（不调 LLM）
    │   └── referral.py      # 转诊模板拼接
    └── response/            # 直接回复用户
        ├── greeting.py      # 寒暄 / 作者彩蛋
        └── fallback.py      # 宽口径兜底（规则判无关时仍可能答心血管问题）
```

### 节点与 Prompt 对应

| route | 节点文件 | LLM Prompt | 静态规则/模板 |
|-------|----------|------------|----------------|
| `symptom` | `nodes/intake/symptom.py` | `prompts/llm/symptom_llm.py` | `prompts/symptom.py`（红旗/缓解词）+ `prompts/fallback.py` |
| `history` | `nodes/intake/history.py` | `prompts/llm/history_llm.py` | `prompts/history.py` + `prompts/fallback.py` |
| `medication` | `nodes/intake/medication.py` | `prompts/llm/medication_llm.py` | `prompts/medication.py` + `prompts/fallback.py` |
| `lab` | `nodes/diagnostics/lab.py` | `prompts/llm/lab_llm.py` | `prompts/lab.py`（危急值词） |
| `greeting` | `nodes/response/greeting.py` | `prompts/llm/greeting_llm.py` | `prompts/fallback.py`（含彩蛋） |
| `fallback` | `nodes/response/fallback.py` | `prompts/llm/fallback_llm.py` | `prompts/fallback.py` |
| — | `nodes/diagnostics/risk.py` | 无 | `prompts/risk.py` |
| — | `nodes/diagnostics/referral.py` | 无 | `prompts/referral.py` |
| 路由 | `routing/dispatch.py` + `rules.py` + `keywords/` | **无** | 纯规则；sticky + 默认 `symptom` |
| 多轮规则 | 各 `*_llm.py` 经 `shared.py` | `prompts/llm/shared.py` | `CONVERSATION_RULES` |

### State 输出与 Java 映射

| Java `GeneralUnderstandingResponse` | `CardiologyState` 字段 |
|-------------------------------------|------------------------|
| `urgency` | `triage_level` |
| `explanation` | `clinical_impression` |
| `advice` | `management_advice` |
| `disclaimer` | `medical_disclaimer` |

---

## 维护指南

| 现象 | 优先改 |
|------|--------|
| 路由误判（如 QRS 进了 fallback） | `routing/keywords/lab.py` + `routing/rules.py` + `tests/test_route_rules.py` |
| 多轮短句丢上下文 | `routing/keywords/symptom.py` + `rules.py` sticky；`symptom.py` 高危 fast path |
| 「一定要去吗」仍贴问卷 | `symptom.py` + `prompts/fallback.py`（`ER_DOUBT_*`） |
| 「我叫什么」应直答不调 LLM | `dialogue_core.build_identity_recall_output` + dispatch `identity_recall` |
| 回忆类问题 LLM 全挂 | `prompts/fallback.py`（`UNKNOWN_RECALL_FALLBACK`） |
| 画像没写入（称呼/慢病） | `dialogue_core.MemoryExtractor` + `tests/test_memory_extractors.py` |
| 铭铭回答风格/内容不对 | 对应 `prompts/llm/*_llm.py` |
| 科普「心电图怎么看」仍跑 risk/referral | `prompts/llm/lab_llm.py` 的 `needs_risk_referral` + `graph/builder.py` 条件边 |
| 红旗该触发没触发 | `prompts/symptom.py` + `nodes/intake/symptom.py` |
| 检查危急值规则 | `prompts/lab.py` + `nodes/diagnostics/lab.py` |
| 新增节点或改图结构 | `graph/builder.py` + 新建 `nodes/` 下文件 |
| 曾祥瑞 / 寒暄彩蛋 | `prompts/fallback.py` + `prompts/llm/greeting_llm.py` |

**路由关键词**只维护 `routing/keywords/`；**优先级 / sticky** 只改 `rules.py`。PMH 布尔字段词表在 `prompts/history.py`（history 节点用）。

---

## 回归评测（eval）

改路由、symptom、prompt 后建议先跑：

```bash
cd services/ai-agent
poetry run python eval/run_eval.py --tier rules    # 不调 API，测 resolve_route
poetry run python -m unittest tests.test_route_rules tests.test_symptom_routing -v
poetry run python eval/run_eval.py --tier e2e      # 需 DEEPSEEK_API_KEY
```

用例：`eval/cases/cardiology_regression.json`（目标扩到 30～50 条）。说明见 `eval/README.md` 与 [docs/memory-context-eval-guide.md](../../docs/memory-context-eval-guide.md)。

---

## 项目结构

```text
services/ai-agent/
├── configuration/                 # Django settings
├── cardiology_chat/
│   ├── views.py · urls.py
│   ├── services/
│   │   └── chat_graph_service.py  # invoke + checkpoint thread_id
│   ├── graph/
│   │   ├── checkpointer.py        # PostgreSQL PostgresSaver
│   │   ├── dialogue_core.py       # MemoryExtractor / DialoguePolicy / ContextBuilder
│   │   ├── routing/
│   │   │   ├── dispatch.py
│   │   │   ├── rules.py
│   │   │   └── keywords/
│   │   ├── nodes/ …
│   ├── memory/                    # 兼容入口 → dialogue_core.MemoryExtractor
│   ├── prompts/
│   │   ├── llm/                   # 各节点 System Prompt（Router 已停用）
│   │   │   └── shared.py
│   │   └── *.py                   # 红旗词、history PMH 词表、fallback…
│   ├── factory/LLMFactory.py
│   ├── middlewares/
│   └── infra/redis_client.py
├── eval/
│   ├── run_eval.py                # rules / e2e 回归
│   └── cases/cardiology_regression.json
├── common/
├── tests/
│   ├── test_route_rules.py
│   ├── test_symptom_routing.py
│   └── test_memory_extractors.py
├── .env.example
└── manage.py
```

---

## 快速开始

### 环境

- Python 3.13+
- Poetry
- DeepSeek API Key
- Redis（内部 token）
- **PostgreSQL**（LangGraph checkpointer；`docker compose up -d postgres`）

### 安装

```bash
cd services/ai-agent
cp .env.example .env
poetry install --no-root
```

### 配置 `.env`

```env
DJANGO_SECRET_KEY=your-secret-key
DEEPSEEK_API_KEY=sk-xxxxxxxx
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_DB=0
POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5432
POSTGRES_USER=cardiology
POSTGRES_PASSWORD=cardiology
POSTGRES_DB=cardiology
```

### 启动

```bash
poetry run python manage.py runserver 0.0.0.0:8000
```

访问：`http://127.0.0.1:8000/api/cardiology/`

---

## API 文档

### POST `/api/cardiology/general-understanding/`

> 仅供 Java Feign 调用，需 `X-Internal-Token` 请求头。

**请求体：**

```json
{
  "uid": "user-001",
  "session": "session-001",
  "message": "我哪里疼你还记得吗"
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `uid` | 是 | 用户 ID，参与 `thread_id`（`uid:session`） |
| `session` | 是 | 会话 ID，checkpointer 线程键 |
| `message` | 是 | 当前轮用户输入（追加到 checkpoint messages） |

**删会话 checkpoint：** `POST /api/cardiology/checkpoint/delete/`（内部 token，body 同 `uid` + `session`）。

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "urgency": "yellow",
    "explanation": "...",
    "advice": "...",
    "disclaimer": "..."
  }
}
```

### 输出字段

| 字段 | 内部字段 | 取值 |
|------|----------|------|
| `urgency` | `triage_level` | `""` / `green` / `yellow` / `red` |
| `explanation` | `clinical_impression` | 主回复 |
| `advice` | `management_advice` | 建议 |
| `disclaimer` | `medical_disclaimer` | 免责声明 |

---

## 多轮对话

```python
# chat_graph_service.py
config = {"configurable": {"thread_id": f"{uid}:{session}"}}
graph.invoke({"messages": [HumanMessage(content=message)]}, config=config)
# 答完后把 explanation 写回 checkpoint
graph.update_state(config, {"messages": [AIMessage(content=explanation)]})
```

| 组件 | 作用 |
|------|------|
| **PostgreSQL checkpointer** | 跨轮持久化 messages 与 state；LLM 从 checkpoint 读上下文 |
| **Java Redis/MySQL 消息** | 仅前端展示；不参与 Feign 上下文 |
| **`conversation_messages_for_llm`** | 调 LLM 时仍只取最近 12 条，assistant 480 字截断 |

图内 `clinical_state`（hpi、pmh、investigation 等）**随 checkpoint 跨轮保留**；删会话或调 `checkpoint/delete/` 时清除。

**本地 smoke test：**

```bash
cd services/ai-agent
poetry run python eval/run_eval.py --tier rules
poetry run python -m unittest tests.test_route_rules tests.test_symptom_routing tests.test_memory_extractors -v
```

---

## 内部鉴权

```text
Java → Redis SET internal:token:{uuid} = ok (TTL 60s)
     → Feign Header: X-Internal-Token
Python → 校验后删除
```

---

## 环境变量

| 变量 | 必填 | 说明 |
|------|------|------|
| `DJANGO_SECRET_KEY` | 是 | Django 密钥 |
| `DEEPSEEK_API_KEY` | 是 | DeepSeek Key |
| `REDIS_HOST` | 是 | 内部 token 校验 |
| `POSTGRES_*` / `POSTGRES_CHECKPOINTER_URI` | 是 | LangGraph checkpointer |
| `REDIS_PORT` | 否 | 默认 6379 |
| `QIANWEN_API_KEY` | 否 | 多模态（规划） |
| `LANGCHAIN_TRACING_V2` | 否 | LangSmith |

---

## 路线图

| 功能 | 状态 |
|------|------|
| LangGraph 意图路由（纯规则 + sticky，无 Router LLM） | ✅ |
| 多轮 LLM 统一架构（shared / conversation_node / fallback） | ✅ |
| lab 条件流水线（科普跳过 risk/referral） | ✅ |
| 用药咨询节点（medication） | ✅ |
| 多轮 session（PostgreSQL checkpointer + LLM 12 条窗口） | ✅ |
| 内部 token 鉴权 | ✅ |
| 寒暄 / 作者彩蛋 | ✅ |
| graph 目录分层（routing / intake / diagnostics / response） | ✅ |
| 回归评测集（rules + e2e） | ✅ `eval/` |
| 结构化记忆 + 上下文构建 + 对话策略（dialogue_core） | ✅ 见 [memory-context-eval-guide.md](../../docs/memory-context-eval-guide.md) |
| 性别/过敏/用药记忆、跨 session profile | 📋 同上 |
| LoRA 微调 + 本地推理 A/B | 📋 [docs/lora-finetune.md](../../docs/lora-finetune.md) |
| reasoning / multimodal（ECG · CTA · 彩超） | 📋 |
| SSE 流式 | 📋 |

---

## 作者

**zengxiangrui**（曾祥瑞） · zengxiangruiit@gmail.com

---

<div align="center">

[← 项目根目录](../../README.md) · [Java 中间层 →](../cardiology-cloud/README.md)

</div>
