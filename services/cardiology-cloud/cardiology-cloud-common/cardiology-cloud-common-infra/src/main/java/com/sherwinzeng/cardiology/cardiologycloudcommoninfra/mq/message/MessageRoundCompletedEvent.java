package com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.message;

import java.time.LocalDateTime;

/**
 * 一轮问诊（user + assistant）完成后的会话索引更新事件。
 */
public record MessageRoundCompletedEvent(
        String eventId,
        String sessionId,
        String uid,
        Long userMessageId,
        Long assistantMessageId,
        String preview,
        int deltaCount,
        LocalDateTime messageAt
) {}
