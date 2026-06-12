# Cardiology Cloud

Spring Cloud Alibaba + Dubbo 微服务模块（待开发）

## 建议目录结构

```text
cardiology-cloud/
├── pom.xml                    # 父 POM
├── gateway/                   # API 网关
├── api/                       # Dubbo API 定义
└── cardiology-service/        # 业务服务（通过 HTTP/Feign 调用 ai-agent）
```

## 与 AI 服务对接

AI 服务运行在 `services/ai-agent`，默认基址：

```text
http://127.0.0.1:8000/api/cardiology/
```

可用接口示例：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/general-understanding/` | 普通医疗对话 |
| POST | `/reasoning/` | 深度推理（开发中） |
| POST | `/multimodal/` | 多模态解读（开发中） |

Java 侧推荐使用 **OpenFeign** 或 **RestTemplate** 调用上述 HTTP 接口，再通过 Dubbo 暴露给上层业务。

## 环境变量建议

在业务服务中配置：

```properties
ai-agent.base-url=http://127.0.0.1:8000
```
