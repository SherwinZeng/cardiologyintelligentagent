# 协作说明

感谢你对 **Cardiology Intelligent Agent Platform（心血管智能问诊 · 就医协助平台）** 的关注。

本项目由作者个人维护，代码以 [Apache License 2.0](LICENSE) 开源，供学习、参考与 fork 使用。

---

## 暂不接受外部 Pull Request

当前 **不接受** 外部提交的 Pull Request，也不会合并第三方 PR。

若你有建议或发现问题，可以通过 [Issue](https://github.com/SherwinZeng/cardiologyintelligentagent/issues) 反馈；**安全相关问题**请按 [SECURITY.md](.github/SECURITY.md) 私下联系，不要在公开 Issue 中披露细节。

你可以自由 fork 本仓库并在自己的 fork 中修改，但请遵守 [LICENSE](LICENSE) 保留版权声明与许可证全文。

---

## 项目目标（供 fork 者参考）

做一个 **能部署、能演示、能持续迭代** 的心血管健康产品：

```text
登录 → 与 AI 助手「铭铭」问诊 → 获得缓急判断与建议
                    ↓
              聊天记录可查、可续聊
                    ↓
           需要就诊 → 提交挂号意向（异步，规划中）
                    ↓
         爬虫代操作公开挂号页 → 成功 / 失败通知用户
```

> **定位**：健康信息辅助与就医引导，**不替代**医生诊断与处方。详见 [README.md](README.md) 免责声明。

---

## 分支与发布

| 分支 | 用途 |
|------|------|
| `dev` | 日常开发 |
| `master` | 稳定版本；通过 PR 或 merge 从 `dev` 合入后发布 |

Git 仓库根目录为 `CardiologyIntelligentAgent/`（`.git` 在根目录，不在 `frontend/` 子目录）。

从 2026-06-18 起，版本 tag 统一使用 `vMAJOR.MINOR.PATCH[-beta.N]`，例如 `v0.2.0-beta.1`、`v0.2.0`、`v0.2.1`。旧 tag（`beta1.2`、`beta1.1`、`0.3` 等）保留为历史，不再继续沿用。

发布前需要同步更新：

- `VERSION`
- `CHANGELOG.md`
- GitHub Release Notes

详细流程见 [docs/release-process.md](docs/release-process.md)。

---

## 本地开发环境

完整步骤见 [README.md · 快速开始](README.md#快速开始)。简要如下：

### 环境要求

JDK 17 · Maven 3.9+ · Node.js 20+ · Yarn · Python 3.13+ · Poetry · Docker

### 1. 中间件

```bash
# 仓库根目录：一键启动 + 导入 Nacos（含 Sentinel）
chmod +x scripts/local-docker-up.sh deploy/nacos-import.sh
./scripts/local-docker-up.sh
```

或 `docker compose up -d` 后执行 `./deploy/nacos-import.sh`。  
认证服务需独立库 `cardiology-auth`（见 README）。  
**本地**：短信在 `cardiology-auth-server.yaml` 填写阿里云 AK 及 `auth.sms`。  
**生产 Docker**：`deploy/.env` + `./deploy/deploy.sh up -d --build`（见 [deploy/README.md](deploy/README.md)）。

### 2. 后端与 AI

```bash
# AI（:8000）
cd services/ai-agent
cp .env.example .env
poetry install --no-root
poetry run python manage.py runserver 0.0.0.0:8000

# 认证 auth（:30002）
cd services/cardiology-cloud/cardiology-auth
mvn spring-boot:run

# 问诊 session（:30001，另开终端）
cd services/cardiology-cloud/cardiology-session
mvn spring-boot:run

# 网关 gateway（:30000，另开终端；依赖 auth / session 已注册 Nacos）
cd services/cardiology-cloud/cardiology-gateway
mvn spring-boot:run
```

### 3. 前端

```bash
cd frontend
yarn install
yarn dev
```

开发默认：前端 `http://127.0.0.1:5173`；Vite 将 `/api` 代理到网关 `:30000`；Axios 基址 `VITE_AUTH_API_BASE_URL=http://127.0.0.1:30000`。  
登录方式：游客一键体验、手机短信验证码（需配置阿里云）；聊天页支持会话列表 / 置顶 / 删除 / 多轮问诊；写操作经 `useRequireAuth` / `useEnsureAuthWithPrompt` 引导登录。

---

## 提交前自检（作者自用）

在 `frontend/` 目录：

```bash
yarn lint
yarn format:check
yarn build
```

Java / Python 模块按各子目录现有测试与构建方式验证。  
**切勿** 提交 `.env`、API Key、JWT 密钥、数据库密码等敏感信息；仅提交 `.env.example` 类模板。

---

## 许可证

贡献至本仓库的代码（若有）及现有代码均按 [Apache License 2.0](LICENSE) 授权。
