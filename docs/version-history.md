# 历史版本整理

这份文件用于把早期不统一的 tag 命名整理成可展示、可维护的版本线。

## 处理原则

- 不删除仓库，不重写提交历史。
- 旧 tag 保留为历史别名，避免破坏已有链接。
- README、CHANGELOG、GitHub Releases 从现在开始只使用规范版本号。
- 后续如确实想清理 GitHub tag 页面，先创建规范 tag，再确认无引用后删除旧 tag。

## 规范版本线

| 规范版本 | 历史 tag / commit | 日期 | 定位 |
|----------|-------------------|------|------|
| `v0.1.0` | `v0.1` / `867866e` | 2026-06-14 | 铭铭基础问答跑通，AI 原型可用 |
| `v0.1.1` | `v0.21` / `f721b85` | 2026-06-16 | 游客认证与项目说明补充 |
| `v0.1.2` | `0.3` / `ad62eeb` | 2026-06-16 | gateway、session、token 认证链路成型 |
| `v0.1.3-beta.1` | `beta1.0` / `684f064` | 2026-06-17 | Docker 生产部署、Nacos、会话能力、record Worker 与短信配置就绪 |
| `v0.1.3-beta.2` | `beta1.1` / `4699ac5` | 2026-06-18 | 前端聊天体验修复，降低页面跳动与输入问题 |
| `v0.1.3-beta.3` | `beta1.2` / `b9dbb60` | 2026-06-18 | 修复前端 CI，停止追踪 `.cursor` |
| `v0.2.0-beta.1` | `a1a0657` 之后 | 2026-06-18 | 上下文历史、Graph 重构、前端体验、GitHub 展示与版本治理 |

## 推荐 GitHub Release 展示方式

GitHub Releases 可以只发布这些规范版本：

1. `v0.1.0`：原型可用
2. `v0.1.3-beta.1`：可部署 beta
3. `v0.1.3-beta.3`：早期 beta 稳定点
4. `v0.2.0-beta.1`：当前求职展示版本

不建议每一个早期小 tag 都发 Release，否则页面会显得琐碎。

## 是否删除旧 tag

默认不删除。

如果你确实想让 GitHub tag 页面更干净，可以在确认规范 tag 已推送后，再删除旧 tag：

```bash
git tag -d v0.1 v0.21 0.3 beta1.0 beta1.1 beta1.2
git push origin :refs/tags/v0.1
git push origin :refs/tags/v0.21
git push origin :refs/tags/0.3
git push origin :refs/tags/beta1.0
git push origin :refs/tags/beta1.1
git push origin :refs/tags/beta1.2
```

这一步属于破坏性清理，只有在你确定不需要旧 tag 链接时再做。
