package com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.message;

import java.time.LocalDateTime;

/**
 * 一轮问诊完成后，延迟生成问诊总结（由 record Worker 写入 Redis 调度，空闲到期后执行）。
 */
public record ConsultationSummaryScheduleEvent(
        String eventId,
        String sessionId,
        String uid,
        LocalDateTime messageAt
) {}
