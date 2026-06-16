package com.sherwinzeng.cardiology.cardiologygateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 游客会话 Redis 配置。
 * <p>
 * 与 cardiology-auth 服务使用相同配置项，保证网关校验的 Redis key 与 auth 写入的一致。
 * <p>
 * 配置示例（注意 key 含冒号，YAML 中必须加引号）：
 * <pre>
 * auth:
 *   guest:
 *     key: "cardiology:guest:session:"
 *     time: 7200
 * </pre>
 * <ul>
 *   <li>{@code key}：Redis 会话 key 前缀，完整 key = key + userId</li>
 *   <li>{@code time}：会话过期秒数（网关侧仅读取 key，time 由 auth 服务写入 Redis TTL 时使用）</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "auth.guest")
public class AuthGuestProperties {

    /** Redis 会话 key 前缀，例如 cardiology:guest:session: */
    private String key;

    /** 会话有效期（秒），与 auth 服务保持一致 */
    private Long time;
}
