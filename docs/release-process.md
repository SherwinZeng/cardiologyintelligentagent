# 版本管理与发布流程

这份规范的目标是让项目长期维护时不再依赖临时判断：分支怎么走、版本号怎么定、什么时候打 tag、GitHub Release 写什么，都按同一套规则执行。

## 分支模型

| 分支 | 用途 | 规则 |
|------|------|------|
| `dev` | 日常开发分支 | 新功能、修复、文档都先合到这里 |
| `master` | 稳定发布分支 | 只接收来自 `dev` 或 `release/*` 的稳定合并 |
| `feature/*` | 功能分支 | 例如 `feature/memory-system`、`feature/milvus-rag` |
| `fix/*` | 修复分支 | 例如 `fix/chat-history-context` |
| `release/*` | 发布准备分支 | 例如 `release/v0.2.0-beta.1` |

个人开发时可以先保持 `dev -> master` 的简单模型；功能变大时再拆 `feature/*`。

## 版本号规则

从现在开始统一使用语义化版本：

```text
vMAJOR.MINOR.PATCH[-beta.N]
```

示例：

| 版本 | 含义 |
|------|------|
| `v0.2.0-beta.1` | 0.2 的第一个 beta，适合求职展示和小范围演示 |
| `v0.2.0` | 0.2 正式稳定版 |
| `v0.2.1` | 0.2 的补丁修复版 |
| `v0.3.0-beta.1` | 0.3 的第一个 beta |

旧 tag（`beta1.2`、`beta1.1`、`0.3` 等）已清理，不再继续使用这种命名。历史版本对照见 [version-history.md](version-history.md)。

## 提交信息

建议使用 Conventional Commits，方便以后生成 Release Notes：

```text
feat(session): send recent chat history to ai-agent
fix(frontend): keep chat input stable while streaming
refactor(ai-agent): split graph runtime modules
docs: document release process
chore(github): add issue templates
```

常用类型：

| 类型 | 用途 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `refactor` | 不改变行为的结构调整 |
| `docs` | 文档 |
| `test` | 测试 |
| `chore` | 构建、CI、仓库配置 |
| `release` | 发布准备 |

## 发布检查清单

发布前在本地确认：

```bash
git status --short
```

前端：

```bash
cd frontend
yarn install
yarn lint
yarn format:check
yarn build
```

Java：

```bash
cd services/cardiology-cloud
mvn -DskipTests compile
```

Python：

```bash
cd services/ai-agent
python -m compileall cardiology_chat common configuration
```

如果本地环境已完整安装 Python 依赖，再追加运行：

```bash
python manage.py check
python -m unittest discover -s tests
```

## 发布步骤

1. 在 `dev` 完成开发并确认工作区干净。
2. 更新 `VERSION` 与 `CHANGELOG.md`。
3. 从 `dev` 合入 `master`，可以用 GitHub Pull Request，也可以个人项目中本地 merge 后 push。
4. 在 `master` 上打规范 tag：

```bash
git tag -a v0.2.0-beta.1 -m "Release v0.2.0-beta.1"
git push origin master --tags
```

5. 在 GitHub Releases 中选择该 tag，发布说明优先复制 `CHANGELOG.md` 对应版本。
6. 发布后回到 `dev` 继续下一个版本。

## GitHub 维护建议

- README 只放项目亮点、截图、架构、快速开始和路线图。
- 版本变化写入 `CHANGELOG.md`，不要散落在聊天记录或临时文档里。
- 技术债、Bug、下一步需求写 Issue，用 label 区分 `bug`、`feature`、`docs`、`release`。
- PR 必须说明改了什么、验证了什么、是否影响发布。
- Secrets、`.env`、真实手机号、真实 token 永远不要提交。

## 当前建议节奏

| 阶段 | 目标 |
|------|------|
| `v0.2.0-beta.1` | 稳定 Web 问诊、历史上下文、GitHub 展示与发布规范 |
| `v0.2.0` | 修完 Java 测试、补齐核心接口文档，作为可投递版本 |
| `v0.3.0-beta.1` | 接入 Milvus/RAG 或多模态检查解读的第一版 |

## 历史版本归档

早期版本统一归档到以下展示线：

| 规范版本 | 历史 tag |
|----------|----------|
| `v0.1.0` | `v0.1` |
| `v0.1.1` | `v0.21` |
| `v0.1.2` | `0.3` |
| `v0.1.3-beta.1` | `beta1.0` |
| `v0.1.3-beta.2` | `beta1.1` |
| `v0.1.3-beta.3` | `beta1.2` |
