# 云服务器部署指南

## 架构

```text
Internet :80
    ↓
frontend (Nginx)
    ├→ 静态资源 dist
    └→ /api/* 反代 → cardiology-session:30001（当前直连，未经网关）
                           └→ ai-agent:8000（内网）
                           ├→ MySQL
                           └→ Redis
Nacos（内网，配置中心）
```

> **注意**：本地开发已启用 `cardiology-gateway :30000` 统一鉴权与路由；生产 Compose 尚未纳入 gateway / auth，后续四期将补齐。

公网只暴露 **frontend 的 80 端口**；MySQL、Redis、Nacos、Java、Python 均在 Docker 内网。

## 服务器要求

- Linux（推荐 Ubuntu 22.04+）
- Docker 24+、Docker Compose v2
- 2 核 4G 内存以上（Nacos + Java + Python 同时运行）
- 已开放安全组 **80**（HTTPS 见下文）

## 首次部署

```bash
# 1. 克隆代码
git clone <your-repo-url> CardiologyIntelligentAgent
cd CardiologyIntelligentAgent

# 2. 配置环境变量
cp deploy/.env.example deploy/.env
# 编辑 deploy/.env，至少修改:
#   MYSQL_ROOT_PASSWORD / MYSQL_PASSWORD
#   NACOS_PASSWORD
#   DJANGO_SECRET_KEY
#   DEEPSEEK_API_KEY

# 3. 构建并启动
chmod +x deploy/deploy.sh
./deploy/deploy.sh up -d --build

# 4. 查看状态
./deploy/deploy.sh ps
./deploy/deploy.sh logs -f frontend
```

浏览器访问：`http://<服务器公网IP>/`

## 冒烟测试

```bash
# 当前生产编排直连 session，无需 JWT
curl -X POST "http://<服务器IP>/api/chat/generalUnderstanding/v1" \
  -H "Content-Type: application/json" \
  -d '{"uid":"user-001","session":"session-001","message":"我胸口疼"}'
```

本地开发（经网关）：

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1:30000/auth/guest/login/v1 \
  -H "Content-Type: application/json" \
  -d '{"guestId":"guest-demo-001"}' | jq -r '.data.token')

curl -X POST http://127.0.0.1:30000/chat/generalUnderstanding/v1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"uid":"user-001","session":"session-001","message":"我胸口疼"}'
```

## 更新发布

```bash
git pull
./deploy/deploy.sh up -d --build
```

## HTTPS（推荐）

生产环境建议在宿主机或独立 Nginx 上配置 TLS：

1. 域名解析到服务器 IP
2. 使用 certbot 申请证书
3. 443 反代到 `127.0.0.1:80`，或改 `FRONTEND_HTTP_PORT` 并在 frontend 前加 TLS 终止层

## Nacos 配置（可选）

`cardiology-session` 已通过环境变量注入数据库、Redis、AI 地址，**可不手动导入 Nacos**。

若仍希望使用 Nacos 管理配置，将 `services/cardiology-cloud/nacos-config/cardiology-session-server.yaml` 中的地址改为 Docker 服务名后导入控制台。

## 常见问题

| 现象 | 排查 |
|------|------|
| 前端空白 | `./deploy/deploy.sh logs frontend` |
| 502 /api | `./deploy/deploy.sh logs cardiology-session ai-agent` |
| AI 无响应 | 检查 `DEEPSEEK_API_KEY` 是否有效 |
| session 启动失败 | 等待 MySQL healthcheck；`logs mysql cardiology-session` |

## 文件说明

| 文件 | 说明 |
|------|------|
| `docker-compose.prod.yaml` | 生产编排 |
| `deploy/.env.example` | 环境变量模板 |
| `deploy/deploy.sh` | compose 封装脚本 |
| `frontend/Dockerfile` | 前端构建 + Nginx |
| `frontend/nginx.conf` | 静态资源 + `/api` 反代 |
| `services/cardiology-cloud/Dockerfile` | Java session 服务 |
| `services/ai-agent/Dockerfile` | Python AI 服务 |
| `docker/mysql/init/02-chat-message.sql` | 消息表初始化 |
