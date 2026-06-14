package com.sherwinzeng.cardiology.cardiologycloudcommoninfra.redis.constant;

public final class RedisCacheNames {

    /** sessionId → uid 归属，多轮记忆校验用 */
    public static final String SESSION_OWNER = "session:owner";

    /** 聊天限流，按 uid */
    public static final String RATE_CHAT = "rate:chat";

    private RedisCacheNames() {
    }
}