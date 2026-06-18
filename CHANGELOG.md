# Changelog

本项目从 2026-06-18 起采用语义化版本管理：`vMAJOR.MINOR.PATCH[-beta.N]`。

历史 tag（如 `beta1.2`、`beta1.1`、`0.3`）保留作为早期里程碑，不再继续沿用。完整对照见 [docs/version-history.md](docs/version-history.md)。

## [Unreleased]

### Added

- `graph/routing/rules.py` **纯规则路由**：图的 conditional edge 仅由 `resolve_route()` 决定（关键词 + 会话语境 + sticky）；移除 dispatch Router LLM。
- 症状多轮：`部分缓解` 不误触 resolved；高危语境下「一定要去吗 / 能不能不去」等急诊犹豫问题走确定性 fast path，不调 LLM，不再贴首轮长问卷。
- `tests/test_route_rules.py`、`tests/test_symptom_routing.py` 路由、symptom 兜底与高危就医确认回归测试。

### Changed

- dispatch：**不再调用** `prompts/llm/router_llm.py`；日志仅 `dispatch 规则路由`。
- `resolve_route()` 永远返回合法 route（默认 `symptom`），不再返回 `None` 走 LLM。
- LangGraph **PostgreSQL checkpointer**（`langgraph-checkpoint-postgres`），`thread_id = uid:session`，跨轮 state 持久化；删会话时 Java Feign 调 `checkpoint/delete/`。
- `docker-compose` 增加 **PostgreSQL** 服务（本地 checkpointer 存储）。
- Java `generalUnderstanding` **不再加载/传递 `history`**；Feign 请求体仅 `uid` / `session` / `message`。
- ai-agent 每轮只 append 当前 `HumanMessage`，答完后 `update_state` 写入 `AIMessage`；多轮上下文从 checkpoint 恢复。
- 移除 `conversation_memory_node`；跨轮短期上下文改为 checkpoint messages + state。

### Next

- **LoRA 微调**：见 [docs/lora-finetune.md](docs/lora-finetune.md)（数据 JSONL → LoRA SFT → `LLMFactory` 本地后端 A/B）。

### Removed

- Feign DTO 字段 `history` / `HistoryTurn`。
- `graph/memory.py`（旧 Java history 窗口记忆抽取）。

## [0.2.0-beta.1] - 2026-06-18

### Added

- Java session 服务向 Python AI Agent 传递最近多轮对话历史，降低模型丢失用户称呼、上下文断裂的概率。
- 前端智能问诊页支持更完整的会话体验，包括最近会话、续聊、记录入口与更稳定的对话区域。
- README 新增 GitHub 预览图，展示欢迎页、登录页与智能问诊核心界面。
- 新增版本管理规范、PR 模板、Issue 模板与发布检查工作流。

### Changed

- 重组 Python LangGraph 运行时，将 graph 构建、路由、LLM、节点与记忆入口拆分得更清晰。
- Java 服务生产配置改为从 Nacos 加载，部署说明同步到文档。
- AI Graph 历史构建与上下文说明文档化，明确 Java 端负责筛选有效 history，Python 端负责构建提示词与推理。

### Fixed

- 修正 GitHub README 预览图在仓库页面无法显示的问题。
- 优化对话页布局与回复展示，减少输入框、消息区跳动。

### Known Gaps

- Java 单元测试体系仍需补齐；当前发布检查先以编译为主。
- 指南 RAG、Milvus、多模态影像解读、支付挂号仍在后续路线图中。

## Historical Version Map

| 规范版本 | 历史 tag | 说明 |
|----------|----------|------|
| `v0.1.0` | `v0.1` | 铭铭基础问答跑通，AI 原型可用 |
| `v0.1.1` | `v0.21` | 游客认证与项目说明补充 |
| `v0.1.2` | `0.3` | gateway、session、token 认证链路成型 |
| `v0.1.3-beta.1` | `beta1.0` | Docker 生产部署、Nacos、会话能力、record Worker 与短信配置就绪 |
| `v0.1.3-beta.2` | `beta1.1` | 前端聊天体验修复 |
| `v0.1.3-beta.3` | `beta1.2` | 修复前端 CI，停止追踪 `.cursor` |
