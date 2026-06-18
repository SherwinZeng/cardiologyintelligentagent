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
│ gateway :30000                                │
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
│ MySQL · Redis · RabbitMQ · Nacos（服务注册）   │
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

cp deploy/.env.example deploy/.env
# 编辑 deploy/.env：MYSQL_*、JWT_SIGN_KEY、DEEPSEEK_API_KEY、ALIYUN_ACCESS_KEY_* 等

chmod +x deploy/deploy.sh
./deploy/deploy.sh up -d --build

./deploy/deploy.sh ps
```

浏览器访问：`http://<服务器公网IP>/`

> 首次构建 ai-agent 可能较久（Java 单次 rebuild 约 20～40 分钟）。Java 服务使用 **`SPRING_PROFILES_ACTIVE=docker`**，注册到 **Nacos**，网关通过 **`lb://`** 服务发现路由。  
> 容器 `Up` 后网关/auth 仍可能需 **1～2 分钟** 才就绪，过早访问可能短暂 502。

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

Docker Hub 超时时，可先经镜像站拉取并 tag，再 `up --build`：

```bash
docker pull docker.m.daocloud.io/library/redis:7.2-alpine
docker tag docker.m.daocloud.io/library/redis:7.2-alpine redis:7.2-alpine
# mysql / rabbitmq / nacos 同理，见常见问题
```

## 配置分层（local vs docker）

| 环境 | Profile | 配置来源 |
|------|---------|----------|
| 本地 `mvn spring-boot:run` | `local` | `application.yml` + Nacos `cardiology-*-server.yaml` |
| Docker 生产 | `docker` | 同上；**`deploy/.env` 注入密钥**（JWT、MySQL、阿里云 AK 等占位符） |

**Nacos 配置仓库路径**：[`services/cardiology-cloud/nacos-config/`](../services/cardiology-cloud/nacos-config/)

- **首次 `up`**：`nacos-init` 容器自动把 4 份 YAML 导入 Nacos，Java 服务再启动。
- **改配置后**：`./deploy/nacos-import.sh` 重新发布，然后重启对应 Java 服务（或 `@RefreshScope` 字段可热刷）。

**`deploy/.env` 必填**：`MYSQL_*`、`RABBITMQ_*`、`JWT_SIGN_KEY`（≥32 字符）、`DEEPSEEK_API_KEY`、`ALIYUN_ACCESS_KEY_*`（短信）。  
Nacos YAML 里用 `${JWT_SIGN_KEY}`、`${SPRING_DATASOURCE_URL}` 等占位符，Docker 环境变量会填入生产值；本地未设则回退到 `127.0.0.1` 默认值。

**不要**设置 `NACOS_USERNAME` / `NACOS_PASSWORD`（生产 Nacos 关闭鉴权，写了反而会 `unknown user`）。

完整环境变量见 [`deploy/.env.example`](.env.example)。

**已知待修（前端）**：AI 回复时聊天区跳动；欢迎页 chip 自动发送后输入框未清空。另：游客重复登录可能 403。

## 冒烟测试

```bash
TOKEN=$(curl -s -X POST "http://<服务器IP>/api/auth/guest/login/v1" \
  -H "Content-Type: application/json" \
  -d '{"id":"guest-demo-001"}' | jq -r '.data.token')

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
git pull
./deploy/deploy.sh up -d --build
```

## HTTPS

1. 域名解析到服务器 IP
2. 使用 certbot 申请证书
3. 443 反代到 `127.0.0.1:80`，或在 frontend 前增加 TLS 终止层

生产 **docker** profile 采用「**本地 YAML 定义路由 + Nacos 做服务注册**」：

- 路由：`nacos-config/cardiology-gateway-server.yaml`（`lb://` + Nacos 注册）
- 注册：各 Java 服务 `discovery.enabled: true`，Compose 注入 `SPRING_CLOUD_NACOS_SERVER_ADDR=nacos:8848`

生产 **Java 配置统一走 Nacos Config**（与本地一致）；`application-docker.yml` 仅负责连接 Nacos。

## Nacos

生产 Compose **默认启动** Nacos（`cardiology-nacos`），Java 服务注册后网关 `lb://` 才能解析实例。

本地开发同样用 Nacos：

```bash
docker compose up -d   # 根目录 docker-compose.yaml
# Java profile=local → application-local.yml，server-addr=127.0.0.1:8848
```

控制台（仅内网调试）：`http://<服务器IP>:8080/nacos`（需在安全组临时开放 8080，或 `docker exec` 进容器访问）。

## 常见问题

| 现象 | 原因 / 处理 |
|------|-------------|
| **502 /api** | Nacos 未 healthy、Java 未注册，或**启动未完成**（等 1～2 分钟）；查 `docker logs cardiology-gateway`；确认 `.env` **无** `NACOS_*` 账号；rebuild Java 四服务 |
| Docker 镜像 not found | 国内 ECS 连不上 Docker Hub → 用 daocloud 等镜像站预拉（见上文） |
| Nacos `unknown user` | `deploy/.env` 里仍有 NACOS 账号密码，删掉后重启 Java |
| Nacos config is empty | `local` profile 未导入远程 YAML，可忽略；`docker` profile 已关闭 Nacos Config |
| 401 登录失败 | `deploy/.env` 中 `JWT_SIGN_KEY` 未改或与 gateway/auth 不一致 |
| 短信发不出 | 配置 `ALIYUN_ACCESS_KEY_ID/SECRET` 后 rebuild auth；`docker logs cardiology-auth \| grep 短信` |
| 前端空白 | `./deploy/deploy.sh logs frontend` |
| AI 无响应 | `DEEPSEEK_API_KEY` |
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
