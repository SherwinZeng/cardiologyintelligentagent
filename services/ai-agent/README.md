# 🫀 Cardiology Intelligent Agent

> **心血管智能问诊 Agent · 铭铭**  
> 基于 Django + LangChain 的心血管健康 AI 助手后端服务  
> Monorepo 路径：`services/ai-agent/`

![Python](https://img.shields.io/badge/Python-3.13+-3776AB?style=flat-square&logo=python&logoColor=white)
![Django](https://img.shields.io/badge/Django-6.0-092E20?style=flat-square&logo=django&logoColor=white)
![DRF](https://img.shields.io/badge/DRF-3.17-red?style=flat-square)
![LangChain](https://img.shields.io/badge/LangChain-1.3-1C3C3C?style=flat-square)
![Poetry](https://img.shields.io/badge/Poetry-依赖管理-60A5FA?style=flat-square)

[快速开始](#-快速开始) · [架构设计](#-架构设计) · [API 文档](#-api-文档) · [环境变量](#-环境变量) · [项目结构](#-项目结构)

---

## ✨ 项目简介

**Cardiology Intelligent Agent** 是一个面向心血管健康场景的 LLM Agent 后端。核心 AI 角色 **「铭铭」** 专注于心脏与心血管相关咨询，支持：

- 症状初步理解与紧急度评估（绿 / 黄 / 红）
- 通俗病因解释与生活方式建议
- 非心血管话题拒答与就医引导
- 结构化 JSON 输出，便于前端直接渲染

> ⚠️ **免责声明**：本项目仅供健康信息参考，不能替代医生诊断与处方。

---

## 🧩 功能概览

| 模块 | 路由 | 模型 | 状态 |
|------|------|------|------|
| 普通医疗对话 | `POST /api/cardiology/general-understanding/` | DeepSeek V4 Flash | ✅ 可用 |
| 深度医疗推理 | `POST /api/cardiology/reasoning/` | DeepSeek V4 Pro | 🚧 开发中 |
| 多模态解读 | `POST /api/cardiology/multimodal/` | 通义千问 3.7 Plus | 🚧 开发中 |

---

## 🏗 架构设计

### 整体分层

```
                         ┌─────────────────────────┐
                         │   客户端 Web / Apifox    │
                         └────────────┬────────────┘
                                      │ POST JSON
                                      ▼
┌─────────────────────────────────────────────────────────────┐
│  configuration/urls.py  ──▶  cardiology_chat/urls.py       │
└──────────────────────────────┬──────────────────────────────┘
                               ▼
                    ┌──────────────────────┐
                    │  View (BaseCachedLLM) │
                    └──────────┬───────────┘
                               │ self.general_model
                               ▼
              ┌────────────────────────────────────┐
              │   general_understanding_service    │
              │  ┌─────────────┐  ┌─────────────┐  │
              │  │ Serializer  │  │ Prompt 铭铭 │  │
              │  └─────────────┘  └─────────────┘  │
              └────────────────┬───────────────────┘
                               │ invoke
                               ▼
              ┌────────────────────────────────────┐
              │   DeepSeek V4 Flash (类级缓存)      │
              │   BaseCachedLLM ──▶ LLMFactory     │
              └────────────────────────────────────┘
```

### 请求处理流程

```
客户端                View                 Service              大模型
  │                    │                     │                    │
  │── POST message ───▶│                     │                    │
  │                    │── general_model ───▶│                    │
  │                    │   (首次经 Factory   │                    │
  │                    │    创建并缓存)      │                    │
  │                    │                     │── 参数校验 ────────│
  │                    │                     │   (失败则 400)   │
  │                    │                     │── 拼 Prompt ─────│
  │                    │                     │── invoke ───────▶│
  │                    │                     │◀── JSON 文本 ────│
  │                    │                     │── JsonParser     │
  │                    │◀── dict 结构化数据 ──│                    │
  │◀── JsonResponse ───│                     │                    │
  │    200             │                     │                    │
```

### 多模型路由策略

```
CardiologyGeneralUnderstandingView
        │
        ▼
  general_model ──▶ LLMFactory ──▶ deepseek-v4-flash  (temperature 0.7)

CardiologyReasoningView
        │
        ▼
  reasoning_model ──▶ LLMFactory ──▶ deepseek-v4-pro  (temperature 0.7)

CardiologyMultimodalView
        │
        ▼
  multimodal_model ──▶ LLMFactory ──▶ qwen3.7-plus  (temperature 0.5)
```

---

## 📁 项目结构

```
services/ai-agent/
├── configuration/                 # Django 项目配置
│   ├── settings.py                # 环境变量、DRF、中间件
│   ├── urls.py                    # 大路由入口
│   └── handler/
│       └── exception_handler.py   # 全局异常格式化
│
├── cardiology_chat/               # 心内科 Agent 应用
│   ├── views.py                   # API 视图
│   ├── urls.py                    # 小路由
│   ├── serializers/
│   │   └── chat_serializer.py     # 请求体验证
│   ├── services/
│   │   └── general_understanding_service.py
│   ├── prompt/
│   │   └── general_prompt.py      # 铭铭 System Prompt
│   └── factory/
│       ├── LLMFactory.py          # 大模型工厂
│       └── AgentFactory.py        # Agent 工厂
│
├── guide/                         # 心血管指南 PDF 知识库
├── guide_loader.py                # 指南 PDF 加载（根目录，方便直接调用）
│
├── common/common_data/            # 公共数据模块
│   ├── exception/chat_exception.py
│   └── response/
│       ├── ResponseCode.py
│       └── ResponseMessage.py
│
├── log/                           # 日志与启动 Banner
├── .env.example                   # 环境变量模板
├── pyproject.toml                 # Poetry 依赖
└── manage.py
```

---

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| Web 框架 | Django 6.0 |
| API | Django REST Framework |
| Agent | LangChain + LangGraph |
| 大模型 | DeepSeek V4（Flash / Pro）、通义千问 3.7 Plus |
| 配置 | python-dotenv |
| 依赖管理 | Poetry |
| 可观测 | LangSmith（可选） |

---

## 🚀 快速开始

### 环境要求

- Python **3.13+**
- Poetry
- DeepSeek API Key（必填）
- 通义千问 API Key（多模态功能需要）

### 1. 克隆并安装依赖

```bash
git clone <your-repo-url>
cd CardiologyIntelligentAgent

# 推荐：在仓库根目录
make install-ai

# 或进入本目录
cd services/ai-agent
poetry install --no-root
```

> 无 Poetry 时：`pip install -r requirements.txt`（需 Python 3.13）

### 2. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env`，至少填入：

```env
DJANGO_SECRET_KEY=your-secret-key
DEEPSEEK_API_KEY=sk-xxxxxxxx
```

### 3. 启动服务

```bash
poetry run python manage.py runserver
```

启动成功后将看到 ASCII Banner：

```
:: zxr-cardiologyintelligentagent :: (v0.1.0)
Server started at http://127.0.0.1:8000
```

### 4. 测试接口

```bash
curl -X POST http://127.0.0.1:8000/api/cardiology/general-understanding/ \
  -H "Content-Type: application/json" \
  -d '{"message": "我最近偶尔心悸，需要注意什么？"}'
```

---

## 📡 API 文档

### 普通医疗对话

```
POST /api/cardiology/general-understanding/
Content-Type: application/json
```

**请求体**

```json
{
  "message": "我最近偶尔心悸，需要注意什么？",
  "token": ""
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `message` | string | ✅ | 用户问诊内容 |
| `token` | string | ❌ | 预留字段 |

**成功响应 `200`**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "urgency": "yellow",
    "explanation": "心悸可能由多种原因引起……",
    "advice": "建议保持规律作息，减少咖啡因摄入……",
    "disclaimer": "温馨提示：铭铭仅为健康信息助手，不能替代医生诊断……"
  }
}
```

**参数错误 `400`**

```json
{
  "code": 400,
  "message": "{'message': [ErrorDetail(string='This field is required.', code='required')]}",
  "data": null
}
```

### 铭铭输出字段说明

| 字段 | 说明 | 示例值 |
|------|------|--------|
| `urgency` | 紧急度评估 | `green` / `yellow` / `red` |
| `explanation` | 病因通俗解释（50～200 字） | 心悸可能由…… |
| `advice` | 行动建议与就诊指引 | 建议减少咖啡因…… |
| `disclaimer` | 医疗免责声明 | 铭铭仅为健康信息助手…… |

---

## 🔐 环境变量

| 变量 | 必填 | 说明 |
|------|------|------|
| `DJANGO_SECRET_KEY` | ✅ | Django 密钥 |
| `DJANGO_DEBUG` | ❌ | 调试模式，默认 `False` |
| `DJANGO_ALLOWED_HOSTS` | ❌ | 允许域名，逗号分隔 |
| `DEEPSEEK_API_KEY` | ✅ | DeepSeek API Key |
| `QIANWEN_API_KEY` | ❌ | 通义千问 Key（多模态） |
| `DEFAULT_TEMPERATURE` | ❌ | 默认温度，默认 `0.7` |
| `DEFAULT_MAX_TOKENS` | ❌ | 默认最大 Token，默认 `2048` |
| `LANGCHAIN_TRACING_V2` | ❌ | 是否开启 LangSmith |
| `LANGCHAIN_API_KEY` | ❌ | LangSmith API Key |
| `LANGCHAIN_PROJECT` | ❌ | LangSmith 项目名 |

---

## 🧠 核心设计说明

### 路由分层

```
configuration/urls.py          → 大路由（/api/cardiology/）
cardiology_chat/urls.py        → 小路由（绑定 View）
views.py                       → 接口入口
services/                      → 业务编排
```

### 模型与 Prompt 分工

| 层级 | 职责 |
|------|------|
| `LLMFactory` | 创建裸模型（API Key、温度、max_tokens） |
| `BaseCachedLLM` | 按场景缓存模型实例 |
| `general_prompt.py` | 维护铭铭角色与 JSON 输出规范 |
| `service` | 校验 → 拼消息 → 调用 → 解析 JSON |

### 异常处理

所有业务异常继承 `ChatBusinessException`，由 `custom_exception_handler` 统一包装为：

```json
{ "code": 400, "message": "...", "data": null }
```

---

## 🗺 路线图

| 阶段 | 内容 | 状态 |
|------|------|------|
| 已完成 | Django + DRF 项目骨架 | ✅ |
| 已完成 | 铭铭 Prompt 心血管专科设定 | ✅ |
| 已完成 | 普通对话接口 + JSON 输出 | ✅ |
| 已完成 | 统一异常与响应码 | ✅ |
| 进行中 | 深度推理接口（DeepSeek Pro） | 🚧 |
| 进行中 | 多模态接口（Qwen 3.7 Plus） | 🚧 |
| 规划中 | SSE 流式输出（BaseStreamAPIView） | 📋 |
| 规划中 | 对话历史与多轮会话 | 📋 |
| 规划中 | RAG 知识库接入 | 📋 |

---

## 👤 作者

**zengxiangrui** · zengxiangruiit@gmail.com

---

<p align="center">🌸 <em>花开堪折直须折，莫待无花空折枝</em> 🌸</p>

<p align="center"><strong>如果这个项目对你有帮助，欢迎 Star ⭐</strong></p>
