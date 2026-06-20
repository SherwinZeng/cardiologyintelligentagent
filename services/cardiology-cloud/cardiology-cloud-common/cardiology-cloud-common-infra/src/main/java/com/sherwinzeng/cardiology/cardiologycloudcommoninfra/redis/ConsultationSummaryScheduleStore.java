package com.sherwinzeng.cardiology.cardiologycloudcommoninfra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@RequiredArgsConstructor
public class ConsultationSummaryScheduleStore {

    public static final String SCHEDULE_ZSET_KEY = "cardiology:consultation-summary:schedule";
    public static final String UID_HASH_KEY = "cardiology:consultation-summary:uid";

    private final StringRedisTemplate stringRedisTemplate;

    public void schedule(String sessionId, String uid, long executeAtEpochMs) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(uid)) {
            return;
        }
        stringRedisTemplate.opsForZSet().add(SCHEDULE_ZSET_KEY, sessionId, executeAtEpochMs);
        stringRedisTemplate.opsForHash().put(UID_HASH_KEY, sessionId, uid);
    }

    /**
     * 取出已到期的 sessionId，并从 ZSET 移除（uid 映射由调用方处理完后 clearUid）。
     */
    public List<String> pollDueSessionIds(int limit) {
        long now = System.currentTimeMillis();
        Set<String> sessionIds = stringRedisTemplate.opsForZSet().rangeByScore(SCHEDULE_ZSET_KEY, 0, now, 0, limit);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        List<String> due = new ArrayList<>(sessionIds);
        stringRedisTemplate.opsForZSet().remove(SCHEDULE_ZSET_KEY, due.toArray());
        return due;
    }

    public String getUid(String sessionId) {
        Object uid = stringRedisTemplate.opsForHash().get(UID_HASH_KEY, sessionId);
        return uid == null ? null : uid.toString();
    }

    public void clearUid(String sessionId) {
        stringRedisTemplate.opsForHash().delete(UID_HASH_KEY, sessionId);
    }

    public long countScheduled() {
        Long size = stringRedisTemplate.opsForZSet().zCard(SCHEDULE_ZSET_KEY);
        return size == null ? 0L : size;
    }
}
