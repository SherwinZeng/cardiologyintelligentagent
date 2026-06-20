# 生产部署

使用 [`docker-compose.prod.yaml`](../docker-compose.prod.yaml) 在 Linux 服务器上一键编排全栈。

## 架构

```text
Internet :80
    │
┌───▼──────────────────────────────────────────┐
│ frontend（Nginx 静态 + /api → gateway）       │
└───┬──────────────────────────────────────────┘
    │
┌───▼──────────────────────────────────────────┐
│ gateway :30000（Sentinel 问诊/Auth 限流）     │
└───┬──────────────────────────────────────────┘
    │
┌───▼──────────────────────────────────────────┐
│ auth :30002 · session :30001 · record Worker  │
└───┬──────────────────────────────────────────┘
    │
┌───▼──────────────────────────────────────────┐
│ ai-agent :8000                                │
└───┬──────────────────────────────────────────┘
    │
┌───▼──────────────────────────────────────────┐
│ MySQL · Redis · PostgreSQL · RabbitMQ · Nacos │
│ （可选 Sentinel Dashboard，仅内网）           │
└──────────────────────────────────────────────┘
```

公网仅暴露 **frontend 的 80 端口**；数据库与中间件在 Docker 内网。

## 服务器要求

- Linux（推荐 Ubuntu 22.04+）
- Docker 24+、Docker Compose v2
- **4 核 16G** 推荐
- 安全组开放 **80**（HTTPS 见下文）

## 首次部署

```bash
git clone https://github.com/SherwinZeng/cardiologyintelligentagent.git CardiologyIntelligentAgent
cd CardiologyIntelligentAgent
git checkout master   # 或 tag v0.3.3-beta.1

cp deploy/.env.example deploy/.env
# 编辑 deploy/.env：MYSQL_*、POSTGRES_*、JWT_SIGN_KEY、DEEPSEEK_API_KEY、ALIYUN_ACCESS_KEY_* 等

chmod +x deploy/deploy.sh scripts/local-docker-up.sh
./deploy/deploy.sh up -d --build
./deploy/deploy.sh ps
```

浏览器访问：`http://<服务器公网IP>/`

> 业务配置在各服务 **`application.yml` / `application-docker.yml`**，无需 `nacos-import`。Nacos 仅做 **`lb://` 服务发现**。  
> 容器 `Up` 后网关/auth 仍可能需 **1～2 分钟** 才就绪，过早访问可能短暂 502。

### 已经上线一半 / 服务器已有旧代码

```bash
git fetch origin
git checkout master && git pull --ff-only

cp -n deploy/.env.example deploy/.env
# 重点补齐/核对：MYSQL_*、POSTGRES_*、RABBITMQ_*、JWT_SIGN_KEY、DJANGO_SECRET_KEY、DEEPSEEK_API_KEY

chmod +x deploy/deploy.sh
./deploy/deploy.sh up -d --build
./deploy/deploy.sh ps
```

Sentinel 限流规则在 gateway 的 `classpath:sentinel-gateway-flow-rules.json`；改后需 **重建 gateway**。详见 [docs/sentinel-setup.md](../docs/sentinel-setup.md)。

这版 AI Agent 使用 PostgreSQL 保存 LangGraph checkpoint；如果旧服务器没有 `postgres` 容器，`up -d --build` 会自动创建。不要执行 `down -v`，否则会删除 MySQL/PostgreSQL 等数据卷。

若从 v0.3.1 之前升级且未跑过问诊记录表，按需执行：

```bash
docker exec -i cardiology-mysql mysql -u cardiology -p cardiology < docker/mysql/migrations/05-consultation-record.sql
```

### 无 Git 更新（打包上传）

```bash
# 本机
tar czf cardiology-deploy.tgz --exclude='.git' --exclude='node_modules' --exclude='frontend/node_modules' --exclude='.cursor' .
scp cardiology-deploy.tgz root@<服务器IP>:~/

# 服务器
cd ~/CardiologyIntelligentAgent && tar xzf ~/cardiology-deploy.tgz
./deploy/deploy.sh up -d --build
```

### 国内 ECS 拉镜像失败

Compose 已默认使用 **daocloud** 镜像（mysql / redis / rabbitmq / postgres / alpine）。若个别镜像仍失败，可预拉并 tag：

```bash
docker pull docker.m.daocloud.io/library/mysql:8.0
docker tag docker.m.daocloud.io/library/mysql:8.0 mysql:8.0
# redis / rabbitmq 同理
```

## 配置分层（local vs docker）

| 环境 | Profile | 配置来源 |
|------|---------|----------|
| 本地 `mvn spring-boot:run` | `local` | `application.yml` + `application-local.yml`（Nacos 仅发现） |
| Docker 生产 | `docker` | `application.yml` + `application-docker.yml`；**`deploy/.env` 注入密钥** |

**配置路径**：各服务 `src/main/resources/application*.yml`；Sentinel 限流见 gateway 的 `sentinel-gateway-flow-rules.json`。

- **改路由 / JWT / 限流**：编辑后 **重建** 对应 Java 服务（gateway 路由变更需重启 gateway）。

**`deploy/.env` 必填**：`MYSQL_*`、`POSTGRES_*`、`RABBITMQ_*`、`JWT_SIGN_KEY`（≥32 字符）、`DJANGO_SECRET_KEY`、`DEEPSEEK_API_KEY`、`ALIYUN_ACCESS_KEY_*`（短信）。

`application-docker.yml` 里 `${JWT_SIGN_KEY}`、`${SPRING_DATASOURCE_URL}` 等由 Docker 环境变量填入；本地未设则回退默认值。

**不要**设置 `NACOS_USERNAME` / `NACOS_PASSWORD`（生产 Nacos 关闭鉴权，写了反而会 `unknown user`）。

完整环境变量见 [`deploy/.env.example`](.env.example)。

**已知待修（前端）**：AI 回复时聊天区跳动；欢迎页 chip 自动发送后输入框未清空。另：游客重复登录可能 403。

## 冒烟测试

```bash
TOKEN=$(curl -s -X POST "http://<服务器IP>/api/auth/guest/login/v1" \
  -H "Content-Type: application/json" \
  -d '{"id":"guest-demo-001"}' | jq -r '.data.token')

curl -X POST "http://<服务器IP>/api/chat/session/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"uid":"guest-demo-001","session":"session-001"}'

curl -X POST "http://<服务器IP>/api/chat/generalUnderstanding/v1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"uid":"guest-demo-001","session":"session-001","message":"我胸口疼"}'
```

## 已有 MySQL 数据卷升级

若服务器上已有旧版 `mysql-data` 卷，按需执行迁移指南见 [`docker/mysql/migrations/`](../docker/mysql/migrations/)。

```bash
docker exec -i cardiology-mysql mysql -u cardiology -p cardiology < docker/mysql/migrations/04-chat-session-lifecycle.sql
```

## 更新发布

```bash
cd ~/CardiologyIntelligentAgent
git fetch origin --tags
git checkout v0.3.3-beta.1   # 或 master
./deploy/deploy.sh up -d --build cardiology-gateway cardiology-session   # 配置有变时重建
# ./deploy/deploy.sh restart cardiology-gateway cardiology-session
```

## Sentinel（v0.3.2+）

| 项 | 说明 |
|----|------|
| 限流位置 | Gateway：`/chat/generalUnderstanding/**` 20 QPS；`/auth/**` 30 QPS |
| 降级 | Session Feign：ai-agent 不可用 → 503 |
| 规则文件 | gateway `classpath:sentinel-gateway-flow-rules.json` |
| 接入文档 | [docs/sentinel-setup.md](../docs/sentinel-setup.md) |
| Dashboard | 生产 Compose 含 `sentinel-dashboard`（**默认不映射公网端口**）；内网调试可临时开放 8858 |

限流 **不依赖** Dashboard；连不上 Dashboard 时日志可能有 WARN，**不影响限流**。

验证：

```bash
# 连发问诊应正常；极端压测才可能 429
curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST "http://<IP>/api/chat/generalUnderstanding/v1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"uid":"guest-demo-001","session":"session-001","message":"测试"}'
```

## HTTPS

1. 域名解析到服务器 IP
2. 使用 certbot 申请证书
3. 443 反代到 `127.0.0.1:80`，或在 frontend 前增加 TLS 终止层

生产 **docker** profile：`application.yml` 定义路由，`Nacos` 仅做 **`lb://` 服务注册**。

- 路由：gateway `application.yml`
- 注册：各 Java 服务 `discovery.enabled: true`，Compose 注入 `SPRING_CLOUD_NACOS_SERVER_ADDR=nacos:8848`

## Nacos

生产 Compose **默认启动** Nacos（`cardiology-nacos`），Java 服务注册后网关 `lb://` 才能解析实例。

本地开发同样用 Nacos：

```bash
docker compose up -d   # 根目录 docker-compose.yaml
# Java profile=local → application-local.yml，server-addr=127.0.0.1:8848
```

控制台（仅内网调试）：`http://<服务器IP>:8080/nacos`（需在安全组临时开放 8080，或 `docker exec` 进容器访问）。

## 本地电脑访问服务器中间件（推荐 SSH 隧道）

生产 Compose 已将 **Nacos / RabbitMQ / Sentinel** 绑定到服务器 **`127.0.0.1`**（不对公网开放）。在你 **本机 Mac** 执行：

```bash
ssh -N -L 8080:127.0.0.1:8080 \
           -L 8848:127.0.0.1:8848 \
           -L 15672:127.0.0.1:15672 \
           -L 8858:127.0.0.1:8858 \
           root@<服务器公网IP>
```

保持该终端不关，然后本机浏览器打开：

| 服务 | 本机地址 | 说明 |
|------|----------|------|
| Nacos 控制台 | http://127.0.0.1:8080/index.html | 生产默认无鉴权 |
| RabbitMQ | http://127.0.0.1:15672 | 账号见 `deploy/.env` 里 `RABBITMQ_*` |
| Sentinel Dashboard | http://127.0.0.1:8858 | `sentinel` / `sentinel` |

**不要**在云安全组对全网开放 8080/8848/15672/8858。若必须公网访问，应加 IP 白名单 + 强鉴权，且仅临时开放。

服务器上若刚改了 `docker-compose.prod.yaml` 端口绑定，需重建中间件：

```bash
./deploy/deploy.sh up -d nacos rabbitmq sentinel-dashboard
```

## 常见问题

| 现象 | 原因 / 处理 |
|------|-------------|
| **502 /api** | Nacos 未 healthy、Java 未注册，或**启动未完成**（等 1～2 分钟）；查 `docker logs cardiology-gateway`；确认 `.env` **无** `NACOS_*` 账号；rebuild Java 四服务 |
| Docker 镜像 not found | 国内 ECS 连不上 Docker Hub → 用 daocloud 等镜像站预拉（见上文） |
| Nacos `unknown user` | `deploy/.env` 里仍有 NACOS 账号密码，删掉后重启 Java |
| Nacos config is empty | 已关闭 Nacos Config，业务配置在 application.yml，可忽略 |
| 401 登录失败 | `deploy/.env` 中 `JWT_SIGN_KEY` 未改或与 gateway/auth 不一致 |
| 短信发不出 | 配置 `ALIYUN_ACCESS_KEY_ID/SECRET` 后 rebuild auth；`docker logs cardiology-auth \| grep 短信` |
| 前端空白 | `./deploy/deploy.sh logs frontend` |
| AI 无响应 | 查 `DEEPSEEK_API_KEY`、`DJANGO_ALLOWED_HOSTS`、`postgres` 健康状态，以及 `./deploy/deploy.sh logs ai-agent` |
| 问诊 429「人数较多」 | Sentinel 限流；改 gateway `sentinel-gateway-flow-rules.json` 中 `count`，重建 gateway |
| 快速切会话也 429 | Gateway 路由未更新：确认 `cardiology-gateway-server.yaml` 含 `cardiology-session-understanding` 路由并重建 gateway |
| Java 反复重启 | `docker logs cardiology-auth`；查 MySQL 密码、fat jar |

## 相关文件

| 文件 | 说明 |
|------|------|
| `docker-compose.prod.yaml` | 生产编排 |
| `deploy/.env.example` | 环境变量模板 |
| `deploy/deploy.sh` | Compose 封装脚本 |
| `frontend/Dockerfile` | 前端构建 + Nginx |
| `services/cardiology-cloud/Dockerfile` | Java 多模块镜像 |
| `services/ai-agent/Dockerfile` | Python AI 服务 |
