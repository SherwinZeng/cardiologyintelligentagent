# Cardiology Intelligent Agent Platform

**心血管智能问诊 · 就医协助平台**

Java 17 · Spring Boot 3.2 · Vue 3 · TypeScript · Python 3.13 · LangGraph · DeepSeek · RabbitMQ · Milvus

---

## 项目愿景

做一个 **能部署、能演示、能持续迭代** 的心血管健康产品。

用户从「我不舒服」出发，由 AI 助手 **铭铭** 完成初步问诊与缓急判断；需要就诊时，系统可 **异步协助挂号**，把「问清楚」和「约得上」连成完整链路。

**定位**：健康信息辅助与就医引导，**不替代**医生诊断与处方。

---

## 系统架构

### 目标架构

```text
用户端 Vue 3 + TypeScript
    ↓
Spring Cloud Gateway + 认证服务
    ↓
Java 业务层（cardiology-session、挂号服务、Sentinel）
    ↓
Python ai-agent（LangGraph 铭铭 + Milvus 向量库）
    ↓
MySQL · Redis · RabbitMQ · Nacos
```

### 当前已实现（MVP）

```text
客户端 → cardiology-gateway :30000（JWT 鉴权）
           ├→ cardiology-auth :30002（游客 / 短信登录）
           └→ cardiology-session :30001
                  ├→ MySQL（chat_session、chat_message）
                  ├→ Redis（内部 token / 游客会话）
                  └→ Feign → ai-agent :8000 → LangGraph 铭铭
```

---

## 核心能力

| 能力 | 说明 | 状态 |
|------|------|------|
| 智能问诊 | LangGraph 分流：症状 / 既往史 / 化验 / 寒暄 / 拒答 | 已完成 |
| 多轮对话 | session 作为 LangGraph thread_id | 已完成 |
| 结构化输出 | urgency / explanation / advice / disclaimer | 已完成 |
| 消息持久化 | 每轮 user + assistant 写入 MySQL | 已完成 |
| 历史查询 | 游标分页（beforeId） | 已完成 |
| 内部鉴权 | Java → Python 一次性 Redis token | 已完成 |
| 游客登录 | JWT 签发、Redis 会话缓存 | 已完成 |
| 短信登录 | 图形验证码 + 阿里云短信 + JWT | 已完成 |
| 网关 | Spring Cloud Gateway 统一入口 | 已完成 |
| Token 鉴权 | 网关 JWT + 游客 Redis 会话校验 | 已完成 |
| 会话管理 | 创建 / 列表 / 搜索 / 置顶 / 删除 | 已完成 |
| 前端聊天 | 多轮问诊、历史加载、会话侧栏 | 已完成 |
| 操作级鉴权 | 未登录引导弹窗、登录回跳 | 已完成 |
| 前端界面 | 登录 / 欢迎 / 聊天 / 帮助 / 隐私；记录页占位；报告 / 挂号待开发 | 进行中 |
| 第三方登录 | QQ、GitHub 等 | 规划中 |
| Token 过期 | 401/403 自动清登录态并引导重登 | 规划中 |
| 指南 RAG | Milvus 向量库检索心血管指南 | 规划中 |
| 熔断限流 | Sentinel 保护 AI / 核心接口 | 规划中 |
| 异步挂号 | RabbitMQ + Seata 多库一致 | 规划中 |
| 结果通知 | RabbitMQ 投递挂号结果通知 | 规划中 |
| 云部署 | Docker + 公网可访问（含 Gateway） | 规划中 |

---

## 业务闭环

```text
登录 → 与铭铭问诊 → 获得缓急判断与建议
                    ↓
              聊天记录可查、可续聊
                    ↓
           需要就诊 → 提交挂号（异步）
                    ↓
         RabbitMQ 处理 → 成功 / 失败通知用户
```

**挂号设计**：RabbitMQ 异步挂号、削峰重试；Seata 保障号源与订单；外部 HIS 最终一致性 + 补偿。

**指南 RAG**：Milvus 存储心血管指南向量，langchain-milvus 检索增强作答。

---

## 子项目

| 项目 | 路径 | 职责 |
|------|------|------|
| Java 中间层 | services/cardiology-cloud | REST API、Feign、落库、微服务 |
| 网关 | cardiology-gateway | 统一入口、JWT 鉴权 |
| 认证 | cardiology-auth | 游客 / 短信登录、JWT |
| 会话 | cardiology-session | 问诊、会话管理、消息历史 |
| Python AI | services/ai-agent | LangGraph 铭铭 |
| 前端 | frontend | Vue 3 + TS |

---

## 技术栈

**前端**：Vue 3 · TypeScript · Vite · Pinia · Element Plus · vue-i18n

**Java**：Spring Boot · Spring Cloud Gateway · Nacos · OpenFeign · MyBatis-Plus · MySQL · Redis · Sentinel · RabbitMQ · Seata

**Python**：Django · DRF · LangGraph · LangChain · DeepSeek V4 Flash · Milvus · Poetry

**运维**：Docker · 云服务器 · HTTPS

---

## 快速开始

**环境**：JDK 17 · Maven · Node.js 20+ · Python 3.13 · Poetry · MySQL · Redis · Nacos

```bash
# 中间件
docker compose up -d

# AI 服务
cd services/ai-agent && poetry install --no-root
poetry run python manage.py runserver 0.0.0.0:8000

# Java 服务（各开终端）
cd services/cardiology-cloud/cardiology-auth && mvn spring-boot:run
cd services/cardiology-cloud/cardiology-session && mvn spring-boot:run
cd services/cardiology-cloud/cardiology-gateway && mvn spring-boot:run

# 前端
cd frontend && yarn install && yarn dev
```

短信登录需在 Nacos `cardiology-auth-server.yaml` 配置阿里云密钥与 `auth.sms` 模板。

**测试**（经网关）：

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1:30000/auth/guest/login/v1 \
  -H "Content-Type: application/json" \
  -d '{"guestId":"guest-demo-001"}' | jq -r '.data.token')

curl -X POST http://127.0.0.1:30000/chat/generalUnderstanding/v1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"uid":"user-001","session":"session-001","message":"我胸口疼"}'
```

---

## API 概览

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | /auth/guest/login/v1 | 无 | 游客登录 |
| POST | /auth/sms/login/captcha/v1 | 无 | 图形验证码 |
| POST | /auth/sms/login/sms/v1 | 无 | 发送短信验证码 |
| POST | /auth/sms/login/v1 | 无 | 短信登录 |
| POST | /chat/session/create | JWT | 创建问诊会话 |
| GET | /chat/session/list/v1 | JWT | 会话列表 |
| POST | /chat/session/pin/v1 | JWT | 置顶会话 |
| DELETE | /chat/session/v1 | JWT | 删除会话 |
| POST | /chat/generalUnderstanding/v1 | JWT | 普通医疗对话 |
| GET | /chat/messages/v1 | JWT | 消息历史（`data.records` + `hasMore`） |

---

## 路线图

| 阶段 | 内容 |
|------|------|
| 一期 · 已完成 | 铭铭问诊 MVP、消息落库、GitHub 开源 |
| 二期 · 已完成 | 游客认证、Gateway、JWT 鉴权、会话创建 |
| 三期 · 已完成 | 前端聊天全流程、会话管理、短信登录、操作级鉴权 |
| 四期 · 进行中 | 记录 / 报告 / 挂号页、Token 过期、第三方登录 |
| 五期 | Sentinel、云部署 |
| 六期 | 异步挂号、结果通知、指南 RAG |
| 远期 | Redis AI 记忆、深度推理、多模态 |

---

## 免责声明

本项目仅供健康信息参考与教育用途，**不能替代**专业医生的诊断、治疗与处方。如有不适，请及时就医。

---

**作者**：zengxiangrui（曾祥瑞） · zengxiangruiit@gmail.com

**仓库**：https://github.com/SherwinZeng/cardiologyintelligentagent
