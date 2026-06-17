package com.sherwinzeng.cardiology.cardiologysession.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 游客聊天会话 Redis 配置，TTL 建议与 auth.guest.time 保持一致。
 */
@Data
@ConfigurationProperties(prefix = "cardiology.guest.chat")
public class GuestChatSessionProperties {

    private String keyPrefix = "cardiology:guest:chat:";

    private long ttlSeconds = 3600;

    private int maxSessions = 5;

    /** 每个 session 最多 user 消息条数 */
    private int maxUserMessages = 30;
}
