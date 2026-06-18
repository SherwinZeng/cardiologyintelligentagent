# Changelog

本项目从 2026-06-18 起采用语义化版本管理：`vMAJOR.MINOR.PATCH[-beta.N]`。

历史 tag（如 `beta1.2`、`beta1.1`、`0.3`）保留作为早期里程碑，不再继续沿用。

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

## Legacy Tags

| Tag | 说明 |
|-----|------|
| `beta1.2` | 早期 beta 版本，作为历史保留 |
| `beta1.1` | 早期 beta 版本，作为历史保留 |
| `beta1.0` | 早期 beta 版本，作为历史保留 |
| `0.3` | 早期非规范 tag，作为历史保留 |
| `v0.21` | 早期非规范 tag，作为历史保留 |
| `v0.1` | 早期非规范 tag，作为历史保留 |
