# Changelog

本项目从 2026-06-18 起采用语义化版本管理：`vMAJOR.MINOR.PATCH[-beta.N]`。

历史 tag（如 `beta1.2`、`beta1.1`、`0.3`）保留作为早期里程碑，不再继续沿用。完整对照见 [docs/version-history.md](docs/version-history.md)。

## [Unreleased]

## [1.0.0] - 2026-06-20

首个正式版：指南 RAG 全栈打通、生产部署文档完善、Docker 生产配置与本地 Milvus 编排就绪。

### Added

- **指南 RAG 参考展示**：ai-agent 返回 `guideReferences`；前端免责声明下方展示中文指南名。
- `chat_message.guide_references` 列（formal MySQL）；guest Redis 消息 JSON 同步字段。
- `GuideReferenceSupport`：List ↔ JSON 字符串编解码（不依赖 MyBatis TypeHandler）。
- `cardiology_chat/rag/`：Milvus 检索、重排、智谱 Embedding、中文指南名映射。
- `scripts/ingest_guides.py`、`scripts/test_rag_retrieval.py`：指南入库与 RAG 验证。
- `scripts/pack-deploy.sh`、`scripts/pull-milvus-images.sh`：打包部署与 Milvus 镜像预拉。
- MySQL 迁移 `06-chat-message-guide-references.sql`。
- Java 四服务 `bootstrap-docker.yml`：Docker 生产关闭 Nacos 配置中心。

### Changed

- **路由**：「做什么检查 / 需要查什么」等优先走 `lab`，避免被 symptom sticky 带偏。
- **本地 Java**：Nacos 配置写入 `bootstrap.yml`；Docker 生产 `bootstrap-docker.yml` + `application-docker.yml`。
- **deploy/README.md**：Milvus/RAG 入库、Postgres 密码、503 排查、磁盘清理、智谱余额等运维说明。
- **README.md** 部署章节：指向 `pack-deploy.sh` 与 RAG 运维文档。
- 本地 `docker-compose.yaml` 增加 Milvus（etcd + minio + standalone）。
- ai-agent `dispatch` 日志输出 RAG 命中与 `guideReferences`。

### Removed

- `cardiology-monorepo.code-workspace`（IDE 工作区文件，改用文件夹直接打开项目）。

## [0.3.3-beta.1] - 2026-06-20

### Added

- `scripts/pack-deploy.sh`：本机打包 `scp` 部署（GitHub 拉不动时用）。
- `scripts/local-docker-up.sh`：本地中间件一键启动。

### Changed

- **配置**：业务改 `application.yml` / `application-docker.yml`；Nacos **仅服务发现**；Sentinel 限流改 gateway `classpath:sentinel-gateway-flow-rules.json`。
- **Docker 生产**：国内 ECS 默认 daocloud 镜像；Nacos/RabbitMQ/Sentinel 管理端口绑定 `127.0.0.1`（SSH 隧道）；[`deploy/README.md`](deploy/README.md) 更新。
- MySQL 增量迁移：`05-consultation-record.sql`（`summary_status` 等 + `consultation_record` 表）。

### Removed

- `nacos-config/`、`nacos-init` 容器、`deploy/nacos-import.sh`；Java 服务移除 `nacos-config` Maven 依赖。

## [0.3.2-beta.1] - 2026-06-20

### Added

- **Sentinel** Gateway 入口限流：Nacos `sentinel-gateway-flow-rules.json`；问诊 `cardiology-session-understanding` 20 QPS、`cardiology-auth` 30 QPS。
- Session **Feign Sentinel 降级**：`DRFAgentFeignFallbackFactory`，ai-agent 不可用时返回 503「铭铭暂时繁忙…」。
- `SentinelGatewayConfiguration`：Gateway 限流统一 429 响应文案。
- [docs/sentinel-setup.md](docs/sentinel-setup.md)：Nacos 三套配置说明（Gateway YAML / 限流 JSON / Session YAML）。
- `docker-compose`：`sentinel-dashboard` 本地可选监控（华为云镜像）。
- `cardiology-monorepo.code-workspace`：多根工作区，便于根目录 README 预览图在子目录工作区中显示。

### Changed

- Gateway 路由拆分：`/chat/generalUnderstanding/**` 独立路由，其余 `/chat/**` 不限流，快速切会话不再误触发 429。
- README **界面预览**：四张截图统一 1800×886（欢迎 / 登录 / 智能问诊 / 问诊记录）。
- `nacos-config`：Gateway / Session Sentinel 数据源；`import.sh` 支持发布 `sentinel-*.json`。

## [0.3.1-beta.1] - 2026-06-20

### Added

- `graph/dialogue_core.py`：集中实现 `MemoryExtractor` / `DialoguePolicy` / `ContextBuilder`，形成结构化记忆、对话策略、上下文构建三层骨架。
- `structured_memory`、`dialogue_policy`、`context_bundle` 三个 LangGraph state 字段，用于持久化用户画像、记录本轮策略、注入 LLM 上下文。
- 身份记忆确定性直答：`我叫什么 / 你怎么称呼我` 直接读取结构化记忆，不调 LLM，不从历史猜；无记忆时明确说明不知道。
- `docs/dialogue-core-line-by-line.md`：`dialogue_core.py` 逐行阅读注释文档，方便学习和后续维护。
- `graph/routing/keywords/`：路由意图词表按场景分文件（greeting / symptom / lab …）。
- `eval/` 多轮回归集与 `run_eval.py`（rules / e2e），rules 层覆盖 route / memory / policy。
- `cardiology_chat/memory/` 兼容入口，内部转发到 `MemoryExtractor`，保留给旧测试和学习用。
- 症状多轮：`部分缓解` 不误触 resolved；高危语境下「一定要去吗 / 能不能不去」等急诊犹豫问题走确定性 fast path，不调 LLM，不再贴首轮长问卷。
- `tests/test_route_rules.py`、`tests/test_symptom_routing.py`、`tests/test_memory_extractors.py` 路由、symptom、结构化记忆与身份回忆回归测试。
- 问诊记录闭环：`cardiology-record` 纯 Worker 扫描 formal 空闲会话，超过 1 小时且消息数大于 20 时调用 ai-agent 生成短标题与摘要，写入 `consultation_record` 并更新会话标题。
- `cardiology-session` 新增 `/chat/record/list/v1`，前端问诊记录页接入真实列表、日期/缓急/关键词筛选、分页与“继续对话”跳转原会话。
- ai-agent 新增内部接口 `session-summary/`，用于问诊记录摘要生成；LLM 不可用时提供确定性兜底摘要。
- `consultation_record` DDL（`05-consultation-record.sql`）与 Redis 延迟调度（`ConsultationSummaryScheduleStore`）。

### Changed

- 问诊记录页前端：6 列表格（主题含图标）、固定分页、筛选与详情抽屉；用户说明（记录生成规则 / 系统小贴士）；主题列图标与文案 i18n。

- dispatch：每轮先执行结构化记忆写入，再计算 route / policy / context；运行时日志输出 route、policy、memory_changed、profile_keys、medical_keys、episode_active。
- LLM 调用：`invoke_llm_json()` 在 system prompt 后注入 `ContextBuilder.as_system_prompt(state)`，让模型看到用户画像和当前症状事件摘要。
- dispatch：**不再调用** `prompts/llm/router_llm.py`。
- `resolve_route()` 永远返回合法 route（默认 `symptom`），不再返回 `None` 走 LLM。
- LangGraph **PostgreSQL checkpointer**（`langgraph-checkpoint-postgres`），`thread_id = uid:session`，跨轮 state 持久化；删会话时 Java Feign 调 `checkpoint/delete/`。
- `docker-compose` 增加 **PostgreSQL** 服务（本地 checkpointer 存储）。
- Java `generalUnderstanding` **不再加载/传递 `history`**；Feign 请求体仅 `uid` / `session` / `message`。
- ai-agent 每轮只 append 当前 `HumanMessage`，答完后 `update_state` 写入 `AIMessage`；多轮上下文从 checkpoint 恢复。
- 结构化记忆补充性别抽取，明确表达“我是男性 / 女性”时写入 profile memory。
- 移除 `conversation_memory_node`；跨轮短期上下文改为 checkpoint messages + state。
- 文档：与 `dialogue_core` 实现对齐——[memory-context-eval-guide.md](docs/memory-context-eval-guide.md)、[dialogue-core-line-by-line.md](docs/dialogue-core-line-by-line.md)、`services/ai-agent/README.md`、`eval/README.md`、`lora-finetune.md`、`beta2-plan.md`。

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
- 多模态影像解读、支付挂号仍在后续路线图中。

## Historical Version Map

| 规范版本 | 历史 tag | 说明 |
|----------|----------|------|
| `v0.1.0` | `v0.1` | 铭铭基础问答跑通，AI 原型可用 |
| `v0.1.1` | `v0.21` | 游客认证与项目说明补充 |
| `v0.1.2` | `0.3` | gateway、session、token 认证链路成型 |
| `v0.1.3-beta.1` | `beta1.0` | Docker 生产部署、Nacos、会话能力、record Worker 与短信配置就绪 |
| `v0.1.3-beta.2` | `beta1.1` | 前端聊天体验修复 |
| `v0.1.3-beta.3` | `beta1.2` | 修复前端 CI，停止追踪 `.cursor` |
