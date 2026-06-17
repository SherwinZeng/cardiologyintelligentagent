package com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.message;

import java.time.LocalDateTime;

/**
 * formal 会话生命周期事件（仅正式用户 MySQL session）。
 * record Worker 归档后发送；record 消费后执行删除等逻辑。
 */
public record SessionLifecycleArchivedEvent(
        String eventId,
        String sessionId,
        String uid,
        LocalDateTime archivedAt
) {
}
