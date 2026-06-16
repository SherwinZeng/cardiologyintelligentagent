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

### 目标架构（规划）

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
客户端 → cardiology-session :30001
           ├→ MySQL（chat_message）
           ├→ Redis（内部 token）
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
| 历史查询 | 按 session 拉取聊天记录 | 已完成 |
| 内部鉴权 | Java → Python 一次性 Redis token | 已完成 |
| 指南 RAG | Milvus 向量库检索心血管指南 | 规划中 |
| 前端界面 | Vue 3 + TypeScript 聊天页 | 规划中 |
| 网关 / 认证 | 统一入口、登录鉴权 | 规划中 |
| 熔断限流 | Sentinel 保护 AI / 核心接口 | 规划中 |
| 异步挂号 | RabbitMQ + Seata 多库一致 | 规划中 |
| 结果通知 | RabbitMQ 投递挂号结果通知 | 规划中 |
| 云部署 | Docker + 公网可访问 | 规划中 |

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
| Java 中间层 | services/cardiology-cloud | REST API、Feign、落库 |
| Python AI | services/ai-agent | LangGraph 铭铭 |
| 前端 | frontend | Vue 3 + TS（待开发） |

---

## 技术栈

**前端（规划）**：Vue 3 · TypeScript · Vite

**Java**：Spring Boot · Spring Cloud · Nacos · OpenFeign · MyBatis-Plus · MySQL · Redis · Sentinel · RabbitMQ · Seata

**Python**：Django · DRF · LangGraph · LangChain · DeepSeek V4 Flash · Milvus · Poetry

**运维（规划）**：Docker · 云服务器 · HTTPS

---

## 快速开始

**环境**：JDK 17 · Maven · Python 3.13 · Poetry · MySQL · Redis · Nacos

```bash
# AI 服务
cd services/ai-agent && poetry install --no-root
poetry run python manage.py runserver 0.0.0.0:8000

# Java 服务
cd services/cardiology-cloud/cardiology-session
mvn spring-boot:run
```

**测试**：

```bash
curl -X POST http://127.0.0.1:30001/chat/generalUnderstanding/v1 \
  -H "Content-Type: application/json" \
  -d '{"uid":"user-001","session":"session-001","message":"我胸口疼"}'
```

---

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /chat/generalUnderstanding/v1 | 普通医疗对话 |
| GET | /chat/messages/v1 | 查询会话消息历史 |

---

## 路线图

| 阶段 | 内容 |
|------|------|
| 一期 · 当前 | 铭铭问诊 MVP、消息落库、GitHub 开源 |
| 二期 | Vue 3 前端、Gateway、认证、Sentinel |
| 三期 | 云部署、线上可演示 |
| 四期 | 异步挂号、结果通知、指南 RAG |
| 远期 | Redis AI 记忆、深度推理、多模态 |

---

## 免责声明

本项目仅供健康信息参考与教育用途，**不能替代**专业医生的诊断、治疗与处方。如有不适，请及时就医。

---

**作者**：zengxiangrui（曾祥瑞） · zengxiangruiit@gmail.com

**仓库**：https://github.com/SherwinZeng/cardiologyintelligentagent
