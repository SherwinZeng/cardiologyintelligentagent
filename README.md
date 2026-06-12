# Cardiology Intelligent Agent Platform

心血管智能问诊分布式系统（Monorepo）

## 仓库结构

```text
.
├── services/
│   ├── ai-agent/              # Python AI 服务（Django + DRF + LangChain + Milvus RAG）
│   └── cardiology-cloud/      # Java 微服务（Spring Cloud Alibaba + Dubbo）
├── frontend/                  # 前端项目（Vue / React 等）
└── README.md
```

## 模块说明

| 目录 | 技术栈 | 职责 |
|------|--------|------|
| `services/ai-agent` | Django、DRF、LangChain、Milvus | 心血管 AI Agent、指南 RAG、结构化 JSON API |
| `services/cardiology-cloud` | Spring Cloud Alibaba、Dubbo、Nacos | 网关、业务编排、通过 HTTP 调用 AI 服务 |
| `frontend` | 待定 | 用户问诊界面 |

## 调用关系

```text
前端  →  Spring Cloud Alibaba（cardiology-cloud）  →  HTTP  →  DRF（ai-agent）
```

## 快速启动

### AI 服务（DRF）

```bash
# 方式一：仓库根目录
make install-ai
make run-ai

# 方式二：进入子目录
cd services/ai-agent
cp .env.example .env   # 填入 API Key
poetry install --no-root
poetry run python manage.py runserver
```

依赖说明：
- Python **3.13**（见 `services/ai-agent/.python-version`）
- 包管理：**Poetry**（虚拟环境在 `services/ai-agent/.venv`）
- 无 Poetry 时可用：`pip install -r services/ai-agent/requirements.txt`

默认地址：`http://127.0.0.1:8000/api/cardiology/`

### Java 微服务

在 `services/cardiology-cloud/` 中初始化 Spring Cloud 项目后启动。

### 前端

在 `frontend/` 中初始化前端项目后启动。

## 免责声明

本项目仅供健康信息参考，不能替代医生诊断与处方。
