# Sentinel 接入指南

Gateway **入口限流** + Session **Feign 降级**，规则持久化在 Nacos。

## 三套配置分工

| # | 配置 | Group | 职责 | 触发场景 |
|---|------|-------|------|----------|
| 1 | `cardiology-gateway-server.yaml` | `DEFAULT_GROUP` | Gateway 路由 + Sentinel 数据源 | 网关启动 |
| 2 | `sentinel-gateway-flow-rules.json` | **`SENTINEL_GROUP`** | **问诊 429** + **Auth 限流** | 超 QPS |
| 3 | `cardiology-session-server.yaml` | `DEFAULT_GROUP` | **Feign 降级**（ai-agent 不可用） | 停 ai-agent → 503 |

**设计要点**：只有 `POST /chat/generalUnderstanding/**` 走 DRF（ai-agent）。会话列表、切会话、拉消息等 **不走 DRF**，因此 **不对整条 `/chat/**` 限流**，只限 `cardiology-session-understanding` 路由。

```text
浏览器
  → Gateway
      ├─ /chat/generalUnderstanding/**  → 限流 20 QPS → session → Feign → ai-agent
      ├─ /chat/**（其余）               → 不限流     → session → MySQL
      └─ /auth/**                       → 限流 30 QPS → auth
```

| 现象 | 来源 | HTTP | 文案 |
|------|------|------|------|
| 问诊太频繁 | Gateway gw-flow | 429 | 当前咨询人数较多，请稍后再试 |
| ai-agent 挂了 | Session Feign Fallback | 503 | 铭铭暂时繁忙… |

---

## Nacos 配置清单

控制台：`http://127.0.0.1:8080` → **配置管理 → 配置列表**

| # | Data ID | Group | 格式 |
|---|---------|-------|------|
| 1 | `cardiology-gateway-server.yaml` | `DEFAULT_GROUP` | YAML |
| 2 | `sentinel-gateway-flow-rules.json` | **`SENTINEL_GROUP`** | JSON |
| 3 | `cardiology-session-server.yaml` | `DEFAULT_GROUP` | YAML |

一键发布（可选）：

```bash
cd services/cardiology-cloud/nacos-config
./import.sh
```

`import.sh` 发布 `*.yaml`（`DEFAULT_GROUP`）和 `sentinel-*.json`（`SENTINEL_GROUP`）。若脚本与 Nacos 版本不兼容，请用手动复制。

---

## 1. Gateway（`cardiology-gateway-server.yaml`）

**DEFAULT_GROUP** · 完整示例（路由顺序不能错：`understanding` 必须在 `session` 前面）：

```yaml
server:
  port: 30000

spring:
  cloud:
    sentinel:
      enabled: true
      eager: true
      transport:
        dashboard: 127.0.0.1:8858
      datasource:
        gw-flow:
          nacos:
            server-addr: 127.0.0.1:8848
            dataId: sentinel-gateway-flow-rules.json
            groupId: SENTINEL_GROUP
            data-type: json
            rule-type: gw-flow
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
      discovery:
        locator:
          enabled: false
      routes:
        - id: cardiology-auth
          uri: lb://cardiology-auth-server
          predicates:
            - Path=/auth/**
          filters:
            - name: Retry
              args:
                retries: 1
                statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
        - id: cardiology-session-understanding
          uri: lb://cardiology-session-server
          predicates:
            - Path=/chat/generalUnderstanding/**
          filters:
            - name: Retry
              args:
                retries: 1
                statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
        - id: cardiology-session
          uri: lb://cardiology-session-server
          predicates:
            - Path=/chat/**
          filters:
            - name: Retry
              args:
                retries: 1
                statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE

auth:
  guest:
    key: "cardiology:guest:session:"

jwt:
  sign-key: "zengxiangrui-daming-cardiology-intelligent-agent-platform"

cardiology:
  auth:
    exclude-paths:
      - /auth/**
```

仓库参考：`services/cardiology-cloud/nacos-config/cardiology-gateway-server.yaml`

---

## 2. 限流规则（`sentinel-gateway-flow-rules.json`）

**SENTINEL_GROUP**（不是 `DEFAULT_GROUP`）· 完整 JSON：

```json
[
  {
    "resource": "cardiology-session-understanding",
    "resourceMode": 0,
    "grade": 1,
    "count": 20,
    "intervalSec": 1,
    "controlBehavior": 0,
    "burst": 0,
    "maxQueueingTimeoutMs": 500,
    "clusterMode": false
  },
  {
    "resource": "cardiology-auth",
    "resourceMode": 0,
    "grade": 1,
    "count": 30,
    "intervalSec": 1,
    "controlBehavior": 0,
    "burst": 0,
    "maxQueueingTimeoutMs": 500,
    "clusterMode": false
  }
]
```

| `resource` | 路由 | QPS | 说明 |
|------------|------|-----|------|
| `cardiology-session-understanding` | `/chat/generalUnderstanding/**` | 20 | 保护 DRF / ai-agent |
| `cardiology-auth` | `/auth/**` | 30 | 登录注册防刷 |

**不要**再对 `cardiology-session`（整条 `/chat/**`）限流，否则快速切会话也会 429。

仓库参考：`services/cardiology-cloud/nacos-config/sentinel-gateway-flow-rules.json`

---

## 3. Session（`cardiology-session-server.yaml`）

**DEFAULT_GROUP** · Sentinel 段（合并进已有 `spring:`，勿重复根节点）：

```yaml
server:
  port: 30001

feign:
  sentinel:
    enabled: true

spring:
  cloud:
    sentinel:
      enabled: true
      eager: true
      transport:
        dashboard: 127.0.0.1:8858
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/cardiology?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: cardiology
    password: cardiology
  data:
    redis:
      host: 127.0.0.1
      port: 6379
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    username: cardiology
    password: cardiology
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10

cardiology:
  ai-agent:
    base-url: http://127.0.0.1:8000/api/cardiology/
  guest:
    chat:
      key-prefix: "cardiology:guest:chat:"
      ttl-seconds: 3600
      max-sessions: 5
      max-user-messages: 30
  mq:
    enabled: true
    session-index:
      exchange: cardiology.session.exchange
      queue: cardiology.session.index.queue
      routing-key: session.index.updated
    session-lifecycle:
      exchange: cardiology.session.exchange
      queue: cardiology.session.lifecycle.queue
      routing-key: session.lifecycle.archived
```

Session **不需要** `gw-flow` 数据源，也 **不需要** 订阅 `sentinel-gateway-flow-rules.json`。

仓库参考：`services/cardiology-cloud/nacos-config/cardiology-session-server.yaml`

---

## 发布与重启

1. Nacos 发布上述 3 份配置  
2. **重启** `cardiology-gateway`（30000）— 路由变更需重启  
3. **重启** `cardiology-session`（30001）  
4. `cardiology-record` 无 HTTP，不必接 Sentinel  

### 启动日志检查

**Gateway 应有：**

- `Load config[dataId=cardiology-gateway-server.yaml] success`
- `register SentinelGatewayFilter`
- `subscribe sentinel-gateway-flow-rules.json+SENTINEL_GROUP`

**Session 应有：**

- `Load config[dataId=cardiology-session-server.yaml] success`
- `register SentinelWebInterceptor`

---

## Sentinel Dashboard（可选）

```bash
docker compose up -d sentinel-dashboard
```

- 地址：http://127.0.0.1:8858  
- 账号：`sentinel` / `sentinel`  

不配 Dashboard 也可以：限流靠 Nacos JSON 生效；连不上时可能打 WARN，**不影响限流**。

---

## 自测

| 测什么 | 怎么做 | 期望 |
|--------|--------|------|
| 切会话不限流 | 快速切换多个会话 | **不**出现 429 |
| 问诊限流 | JSON 里 `cardiology-session-understanding` 的 `count` 改为 `1`，发布，重启 Gateway，连发 2 次问诊 | 第二次 `code: 429` |
| Feign 降级 | 停掉 ai-agent，发问诊 | `code: 503`，铭铭暂时繁忙 |

测完记得把 `count` 改回 `20`。

---

## 生产建议

| 项 | 建议 |
|----|------|
| `cardiology-session-understanding` QPS | 10～30（按 ai-agent 容量调） |
| `cardiology-auth` QPS | 20～50 |
| Dashboard | 可选 |

游客 30 条消息上限是业务规则，与 Sentinel 互补。

---

## 相关代码

- `cardiology-gateway/.../SentinelGatewayConfiguration.java` — 429 响应体
- `cardiology-session/.../DRFAgentFeignFallbackFactory.java` — 503 降级
- `services/cardiology-cloud/nacos-config/` — 上述 3 份配置模板
