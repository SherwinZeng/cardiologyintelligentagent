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
│ ai-agent :8000（LangGraph checkpoint → PG）   │
└───┬──────────────────────────────────────────┘
    │
┌───▼──────────────────────────────────────────┐
│ MySQL · Redis · PostgreSQL · RabbitMQ · Nacos │
│ Milvus（etcd + minio + standalone，指南 RAG） │
│ （Sentinel Dashboard，仅内网 SSH 隧道）       │
└──────────────────────────────────────────────┘
```

公网仅暴露 **frontend 的 80 端口**；数据库与中间件在 Docker 内网。

## 服务器要求

- Linux（推荐 Ubuntu 22.04+）
- Docker 24+、Docker Compose v2
- **4 核 16G** 推荐；系统盘建议 **≥70G**（ai-agent 镜像含 PyTorch 等依赖，首次 `--build` 占用大）
- 安全组开放 **80**（HTTPS 见下文）

## 首次部署

```bash
git clone https://github.com/SherwinZeng/cardiologyintelligentagent.git CardiologyIntelligentAgent
cd CardiologyIntelligentAgent
git checkout master   # 或 tag v1.0.0

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
# 本机（推荐脚本，已排除 node_modules / target / deploy/.env）
chmod +x scripts/pack-deploy.sh
./scripts/pack-deploy.sh
scp cardiology-deploy.tgz root@<服务器IP>:~/

# 服务器
mkdir -p ~/CardiologyIntelligentAgent && cd ~/CardiologyIntelligentAgent
tar xzf ~/cardiology-deploy.tgz
cp -n deploy/.env.example deploy/.env   # 首次；已有 deploy/.env 勿覆盖
vim deploy/.env
./deploy/deploy.sh up -d --build
```

Mac 打包后在 Linux 解压时可能出现大量 `LIBARCHIVE.xattr.com.apple.*` 警告，**可忽略**，不影响文件内容。

### 国内 ECS 拉镜像失败

Compose 已默认使用 **daocloud** 镜像（mysql / redis / rabbitmq / postgres / alpine）。**Milvus 相关镜像**（`milvusdb/milvus`、`milvusdb/etcd`、`minio/minio`）仍可能直连 Docker Hub 失败，可配置 registry mirror 后重启 Docker：

```bash
sudo tee /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.m.daocloud.io"
  ]
}
EOF
sudo systemctl daemon-reload && sudo systemctl restart docker
docker info | grep -A3 "Registry Mirrors"
```

若个别基础镜像仍失败，可预拉并 tag：

```bash
docker pull docker.m.daocloud.io/library/mysql:8.0
docker tag docker.m.daocloud.io/library/mysql:8.0 mysql:8.0
# redis / rabbitmq 同理
```

### 磁盘不足（build 中途 no space left on device）

ai-agent 构建层较大。Build 失败后可清缓存 **（不删数据卷）**：

```bash
docker builder prune -af    # 通常可释放 10～20G build cache
docker image prune -af        # 未使用的镜像（可选）
df -h /
./deploy/deploy.sh up -d --build ai-agent   # 其它服务已 build 过时可只重建 ai-agent
```

## 配置分层（local vs docker）

| 环境 | Profile | 配置来源 |
|------|---------|----------|
| 本地 `mvn spring-boot:run` | `local` | `application.yml` + `application-local.yml`（Nacos 仅发现） |
| Docker 生产 | `docker` | `application.yml` + `application-docker.yml`；**`deploy/.env` 注入密钥** |

**配置路径**：各服务 `src/main/resources/application*.yml`；Sentinel 限流见 gateway 的 `sentinel-gateway-flow-rules.json`。

- **改路由 / JWT / 限流**：编辑后 **重建** 对应 Java 服务（gateway 路由变更需重启 gateway）。

**`deploy/.env` 必填**：`MYSQL_*`、`POSTGRES_*`、`RABBITMQ_*`、`JWT_SIGN_KEY`（≥32 字符）、`DJANGO_SECRET_KEY`、`DEEPSEEK_API_KEY`、`ALIYUN_ACCESS_KEY_*`（短信）。

**指南 RAG 额外建议**：`ZHIPU_API_KEY`（Embedding + 检索/query 时扣费）；`UNSTRUCTURED_API_KEY`（仅在服务器上重新解析 PDF 时需要，见下文入库章节）。

`application-docker.yml` 里 `${JWT_SIGN_KEY}`、`${SPRING_DATASOURCE_URL}` 等由 Docker 环境变量填入；本地未设则回退默认值。

**不要**设置 `NACOS_USERNAME` / `NACOS_PASSWORD`（生产 Nacos 关闭鉴权，写了反而会 `unknown user`）。

完整环境变量见 [`deploy/.env.example`](.env.example)。

**已知待修（前端）**：AI 回复时聊天区跳动；欢迎页 chip 自动发送后输入框未清空。另：游客重复登录可能 403。

## 指南 RAG 入库（首次 / 更新 PDF 后）

**Milvus**：Compose 启动 `milvus-etcd` / `milvus-minio` / `milvus`（不对公网暴露 19530）。`ai-agent` 内 `MILVUS_URI=http://milvus:19530`。

### 检查 `guide_references` 字段（正式用户历史消息）

全新 MySQL 卷（`docker/mysql/init/03-chat-message.sql`）已含该列；**旧卷升级**需 migration：

```bash
source deploy/.env
docker exec cardiology-mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e \
  "SHOW COLUMNS FROM chat_message LIKE 'guide_references';"
# Empty set 时执行：
docker exec -i cardiology-mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" \
  < docker/mysql/migrations/06-chat-message-guide-references.sql
```

### 入库方式 A：本机已解析缓存（推荐，跳过 Unstructured）

Mac 本地若已有 `services/ai-agent/.cache/parsed_guides.pkl`（Unstructured 在本机跑通过）：

```bash
# Mac
scp services/ai-agent/.cache/parsed_guides.pkl root@<IP>:/tmp/parsed_guides.pkl

# 服务器
docker exec cardiology-ai-agent mkdir -p /app/.cache
docker cp /tmp/parsed_guides.pkl cardiology-ai-agent:/app/.cache/parsed_guides.pkl
docker exec cardiology-ai-agent poetry run python scripts/ingest_guides.py --recreate --skip-parse
```

仅需 **`ZHIPU_API_KEY`** 有余额（入库时批量 Embedding）；**不需** `UNSTRUCTURED_API_KEY`。

### 入库方式 B：在服务器上解析 PDF

```bash
docker exec cardiology-ai-agent poetry run python scripts/ingest_guides.py --recreate
```

需 `ZHIPU_API_KEY` + `UNSTRUCTURED_API_KEY`。若 Unstructured 返回 `422 File does not appear to be a valid PDF`，优先改用 **方式 A**。

### 验证

```bash
docker exec cardiology-ai-agent poetry run python scripts/test_rag_retrieval.py "高血压要做什么检查" --dispatch
# 期望：route=lab，guide_references 含「国家基层高血压防治管理指南（2025版）」
```

页面冒烟：登录后问「高血压要做什么检查」，回复底部应出现 **参考指南：…**。

### 查看切块 / Milvus 容量

```bash
# collection 条数
docker exec cardiology-ai-agent poetry run python -c "
from pymilvus import MilvusClient
print(MilvusClient(uri='http://milvus:19530').get_collection_stats('cardiology_guides'))
"

# Docker 卷占用（需 root）
sudo du -sh /var/lib/docker/volumes/cardiology-prod_milvus-*/_data 2>/dev/null

# 解析缓存 pkl（约 1～2 MB / 千块级）
docker exec cardiology-ai-agent ls -lh /app/.cache/parsed_guides.pkl 2>/dev/null || true
du -sh services/ai-agent/guide/    # PDF 原文约 26 MB / 11 个文件
```

## 第三方 API 余额

| 服务 | 用途 | 控制台 | 说明 |
|------|------|--------|------|
| **智谱** | Embedding 入库 + 每次 RAG 检索 | [open.bigmodel.cn](https://open.bigmodel.cn) → 财务 | 429「余额不足」→ 充值即可，**同一 API Key 无需重启容器** |
| **Unstructured** | 服务器上 PDF 切块（方式 B） | [platform.unstructured.io](https://platform.unstructured.io) | 国内访问可能不稳定；已用 pkl 时可不依赖 |
| **DeepSeek** | 对话 LLM | DeepSeek 控制台 | 与 RAG 检索无关 |

智谱 `embedding-3` 约 **0.5 元 / 百万 tokens**。充值后直接在服务器重跑 `test_rag_retrieval.py` 验证。

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

## 运维清理

| 目的 | 命令 |
|------|------|
| 腾磁盘（不动数据卷） | `docker builder prune -af` · `docker image prune -af` |
| 停服务（保留卷） | `./deploy/deploy.sh down` → 再 `up -d` |
| 删部署包 | `rm -f ~/cardiology-deploy.tgz /tmp/parsed_guides.pkl` |
| **彻底重装（删 MySQL/PG/Milvus 等全部数据）** | `./deploy/deploy.sh down` 后 `docker volume rm cardiology-prod_mysql-data cardiology-prod_postgres-data …`（`docker volume ls \| grep cardiology-prod` 核对名称），再 `up -d --build` |
| 仅重建 Milvus 向量 | `ingest_guides.py --recreate --skip-parse`（需 pkl） |

**不要**对生产随意执行 `down -v`，会删除所有命名卷。

## 已有 MySQL 数据卷升级

若服务器上已有旧版 `mysql-data` 卷，按需执行迁移指南见 [`docker/mysql/migrations/`](../docker/mysql/migrations/)。

```bash
docker exec -i cardiology-mysql mysql -u cardiology -p cardiology < docker/mysql/migrations/04-chat-session-lifecycle.sql
```

## 更新发布

```bash
cd ~/CardiologyIntelligentAgent
git fetch origin --tags
git checkout v1.0.0   # 或 master
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
| **503「铭铭暂时繁忙」** | session Feign 调 ai-agent 失败。查 `docker logs cardiology-session \| grep 降级` 与 `docker logs cardiology-ai-agent --tail 50` |
| ai-agent **500** + `password authentication failed for user "cardiology"` | **`deploy/.env` 里 `POSTGRES_PASSWORD` 与 Postgres 数据卷初次初始化密码不一致**（改 `.env` 不会自动改卷内密码）。对齐密码后重启 ai-agent：<br>`docker exec cardiology-postgres psql -U cardiology -d cardiology -c "ALTER USER cardiology WITH PASSWORD '你的POSTGRES_PASSWORD';"`<br>`./deploy/deploy.sh up -d ai-agent` |
| ai-agent **PoolTimeout** / 连不上 PG | 同上；或 postgres 未 healthy |
| RAG **429** 智谱余额不足 | [open.bigmodel.cn](https://open.bigmodel.cn) 充值，**不换 key、不重启**；重跑 `test_rag_retrieval.py` |
| RAG 无「参考指南」 | 未完成 Milvus 入库；或智谱 429；或 route 不在 RAG 范围。查 `docker logs cardiology-ai-agent \| grep guide` |
| Unstructured **422** PDF 无效 | 优先用本机 `parsed_guides.pkl` + `--skip-parse`；或逐个 PDF 用 `--pdf 文件名片段` 排查 |
| build **no space left on device** | `docker builder prune -af`，见上文「磁盘不足」 |
| Mac tar **LIBARCHIVE.xattr** 警告 | 可忽略 |
| Docker 镜像 **not found**（Milvus 等） | 配置 `registry-mirrors` 后 `systemctl restart docker`，见上文 |
| Nacos `unknown user` | `deploy/.env` 里仍有 NACOS 账号密码，删掉后重启 Java |
| Nacos config is empty | 已关闭 Nacos Config，业务配置在 application.yml，可忽略 |
| 401 登录失败 | `deploy/.env` 中 `JWT_SIGN_KEY` 未改或与 gateway/auth 不一致 |
| 短信发不出 | 配置 `ALIYUN_ACCESS_KEY_ID/SECRET` 后 rebuild auth；`docker logs cardiology-auth \| grep 短信` |
| 前端空白 | `./deploy/deploy.sh logs frontend` |
| 问诊 429「人数较多」 | Sentinel 限流；改 gateway `sentinel-gateway-flow-rules.json` 中 `count`，重建 gateway |
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
| `services/ai-agent/scripts/ingest_guides.py` | 指南 PDF → Milvus 入库 |
| `services/ai-agent/scripts/test_rag_retrieval.py` | RAG / dispatch 验证 |
| `scripts/pack-deploy.sh` | 本机打包 `cardiology-deploy.tgz` |
| `docker/mysql/migrations/06-chat-message-guide-references.sql` | 正式用户消息「参考指南」字段 |
