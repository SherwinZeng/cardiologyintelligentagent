package com.sherwinzeng.cardiology.cardiologysession.store;

import com.sherwinzeng.cardiology.cardiologycloudcommondata.exception.ChatBusinessException;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.entity.chat.ChatMessageRole;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.entity.chat.ChatSessionStatus;
import com.sherwinzeng.cardiology.cardiologysession.properties.GuestChatSessionProperties;
import com.sherwinzeng.cardiology.cardiologysession.support.GuestChatRedisKeys;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class GuestChatSessionStore {

    private static final String DEFAULT_TITLE = "新建会话";

    /**
     * Lua：会话数已满
     */
    public static final long CODE_SESSION_LIMIT = -1L;
    /**
     * Lua：会话已存在
     */
    public static final long CODE_SESSION_EXISTS = -2L;
    /**
     * Lua：成功
     */
    public static final long CODE_OK = 1L;

    /**
     * Lua：问题数已满
     */
    public static final long CODE_MESSAGE_LIMIT = -2L;
    /**
     * Lua：会话不存在
     */
    public static final long CODE_SESSION_NOT_FOUND = -3L;

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> guestCreateSessionScript;
    private final DefaultRedisScript<Long> guestAppendUserMessageScript;
    private final GuestChatSessionProperties guestChatSessionProperties;

    public GuestChatSessionStore(StringRedisTemplate stringRedisTemplate, @Qualifier("guestCreateSessionScript") DefaultRedisScript<Long> guestCreateSessionScript,
                                 @Qualifier("guestAppendUserMessageScript") DefaultRedisScript<Long> guestAppendUserMessageScript, GuestChatSessionProperties guestChatSessionProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.guestCreateSessionScript = guestCreateSessionScript;
        this.guestAppendUserMessageScript = guestAppendUserMessageScript;
        this.guestChatSessionProperties = guestChatSessionProperties;
    }

    public ChatSession createSession(String uid, String sessionId) {
        String indexKey = GuestChatRedisKeys.indexKey(guestChatSessionProperties, uid);
        String metaKey = GuestChatRedisKeys.metaKey(guestChatSessionProperties, uid, sessionId);
        String msgsKey = GuestChatRedisKeys.msgsKey(guestChatSessionProperties, uid, sessionId);
        String nowTs = String.valueOf(System.currentTimeMillis());

        Long code = stringRedisTemplate.execute(guestCreateSessionScript, List.of(indexKey, metaKey, msgsKey), sessionId, String.valueOf(guestChatSessionProperties.getTtlSeconds()), String.valueOf(guestChatSessionProperties.getMaxSessions()),
                nowTs, DEFAULT_TITLE, uid);

        if (code == null) {
            throw new ChatBusinessException(ResponseCode.SERVER_ERROR, "创建会话失败，请稍后重试");
        }
        if (code == CODE_SESSION_LIMIT) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "最多 5 个对话，请先删除后再创建");
        }
        if (code == CODE_SESSION_EXISTS) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "会话已存在");
        }
        if (code != CODE_OK) {
            throw new ChatBusinessException(ResponseCode.SERVER_ERROR, "创建会话失败，请稍后重试");
        }
        return toChatSession(uid, sessionId, stringRedisTemplate.<String, String>opsForHash().entries(metaKey));
    }

    public List<ChatSession> listSessions(String uid, String keyword) {
        String indexKey = GuestChatRedisKeys.indexKey(guestChatSessionProperties, uid);
        Set<String> sessionIds = stringRedisTemplate.opsForZSet().reverseRange(indexKey, 0, -1);
        if (sessionIds == null || sessionIds.isEmpty()) return Collections.emptyList();
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<ChatSession> sessions = new ArrayList<>();
        for (String sessionId : sessionIds) {
            String metaKey = GuestChatRedisKeys.metaKey(guestChatSessionProperties, uid, sessionId);
            Map<String, String> meta = stringRedisTemplate.<String, String>opsForHash().entries(metaKey);
            if (meta.isEmpty()) {
                continue;
            }
            ChatSession session = toChatSession(uid, sessionId, meta);
            if (trimmedKeyword != null && !matchesKeyword(session, trimmedKeyword)) {
                continue;
            }
            sessions.add(session);
        }

        sessions.sort(Comparator.comparing(ChatSession::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return sessions;
    }

    public void deleteSession(String uid, String sessionId) {
        String metaKey = GuestChatRedisKeys.metaKey(guestChatSessionProperties, uid, sessionId);
        Map<String, String> meta = stringRedisTemplate.<String, String>opsForHash().entries(metaKey);
        if (meta.isEmpty()) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "会话不存在");
        }
        String ownerUid = meta.get("uid");
        if (!uid.equals(ownerUid)) {
            throw new ChatBusinessException(ResponseCode.FORBIDDEN, "无权删除该会话");
        }

        String indexKey = GuestChatRedisKeys.indexKey(guestChatSessionProperties, uid);
        String msgsKey = GuestChatRedisKeys.msgsKey(guestChatSessionProperties, uid, sessionId);
        stringRedisTemplate.opsForZSet().remove(indexKey, sessionId);
        stringRedisTemplate.delete(metaKey);
        stringRedisTemplate.delete(msgsKey);
    }

    public void appendUserMessage(String uid, String sessionId, String content) {
        String indexKey = GuestChatRedisKeys.indexKey(guestChatSessionProperties, uid);
        String metaKey = GuestChatRedisKeys.metaKey(guestChatSessionProperties, uid, sessionId);
        String msgsKey = GuestChatRedisKeys.msgsKey(guestChatSessionProperties, uid, sessionId);
        long now = System.currentTimeMillis();
        GuestChatMessagePayload payload = new GuestChatMessagePayload();
        payload.setId(now);
        payload.setRole(ChatMessageRole.USER);
        payload.setContent(content);
        payload.setCreatedAt(now);
        Long code = stringRedisTemplate.execute(guestAppendUserMessageScript, List.of(indexKey, metaKey, msgsKey), String.valueOf(guestChatSessionProperties.getTtlSeconds()),
                String.valueOf(guestChatSessionProperties.getMaxUserMessages()), String.valueOf(now), sessionId, JsonSerialization.toJson(payload));
        if (code == null) {
            throw new ChatBusinessException(ResponseCode.SERVER_ERROR, "发送失败，请稍后重试");
        }
        if (code == CODE_MESSAGE_LIMIT) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "本轮对话已达 30 个问题上限");
        }
        if (code == CODE_SESSION_NOT_FOUND) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "会话不存在或已过期");
        }
        if (code != CODE_OK) {
            throw new ChatBusinessException(ResponseCode.SERVER_ERROR, "发送失败，请稍后重试");
        }
    }

    public void appendAssistantMessage(String uid, String sessionId, String content, String urgency,
                                       String explanation, String advice, String disclaimer,
                                       List<String> guideReferences) {
        String indexKey = GuestChatRedisKeys.indexKey(guestChatSessionProperties, uid);
        String metaKey = GuestChatRedisKeys.metaKey(guestChatSessionProperties, uid, sessionId);
        String msgsKey = GuestChatRedisKeys.msgsKey(guestChatSessionProperties, uid, sessionId);
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(metaKey))) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "会话不存在或已过期");
        }
        long now = System.currentTimeMillis();
        GuestChatMessagePayload payload = new GuestChatMessagePayload();
        payload.setId(now);
        payload.setRole(ChatMessageRole.ASSISTANT);
        payload.setContent(content);
        payload.setUrgency(urgency);
        payload.setExplanation(explanation);
        payload.setAdvice(advice);
        payload.setDisclaimer(disclaimer);
        payload.setGuideReferences(guideReferences);
        payload.setCreatedAt(now);
        stringRedisTemplate.opsForList().leftPush(msgsKey, JsonSerialization.toJson(payload));
        stringRedisTemplate.opsForHash().increment(metaKey, "messageCount", 1);
        String preview = buildPreview(content);
        stringRedisTemplate.opsForHash().put(metaKey, "preview", preview);
        stringRedisTemplate.opsForHash().put(metaKey, "updatedAt", String.valueOf(now));
        stringRedisTemplate.opsForZSet().add(indexKey, sessionId, now);
        refreshTtl(uid, sessionId);
    }

    public List<GuestChatMessagePayload> listMessages(String uid, String sessionId, Long beforeId, int fetchSize) {
        String msgsKey = GuestChatRedisKeys.msgsKey(guestChatSessionProperties, uid, sessionId);
        List<String> rawMessages = stringRedisTemplate.opsForList().range(msgsKey, 0, -1);
        if (rawMessages == null || rawMessages.isEmpty()) {
            return Collections.emptyList();
        }
        List<GuestChatMessagePayload> allMessages = new ArrayList<>();
        for (String raw : rawMessages) {
            GuestChatMessagePayload payload = JsonSerialization.fromJson(raw, GuestChatMessagePayload.class);
            if (payload != null) {
                allMessages.add(payload);
            }
        }
        allMessages.sort(Comparator.comparingLong(GuestChatMessagePayload::getId).reversed());
        List<GuestChatMessagePayload> filtered = new ArrayList<>();
        for (GuestChatMessagePayload message : allMessages) {
            if (beforeId != null && beforeId > 0 && message.getId() >= beforeId) {
                continue;
            }
            filtered.add(message);
        }
        int end = Math.min(fetchSize, filtered.size());
        List<GuestChatMessagePayload> page = new ArrayList<>(filtered.subList(0, end));
        Collections.reverse(page);
        return page;
    }

    public long countMessages(String uid, String sessionId) {
        String msgsKey = GuestChatRedisKeys.msgsKey(guestChatSessionProperties, uid, sessionId);
        Long size = stringRedisTemplate.opsForList().size(msgsKey);
        return size == null ? 0 : size;
    }

    public void refreshTtl(String uid, String sessionId) {
        long ttl = guestChatSessionProperties.getTtlSeconds();
        stringRedisTemplate.expire(GuestChatRedisKeys.indexKey(guestChatSessionProperties, uid), ttl, TimeUnit.SECONDS);
        stringRedisTemplate.expire(GuestChatRedisKeys.metaKey(guestChatSessionProperties, uid, sessionId), ttl, TimeUnit.SECONDS);
        stringRedisTemplate.expire(GuestChatRedisKeys.msgsKey(guestChatSessionProperties, uid, sessionId), ttl, TimeUnit.SECONDS);
    }

    private boolean matchesKeyword(ChatSession session, String keyword) {
        return containsIgnoreCase(session.getTitle(), keyword) || containsIgnoreCase(session.getPreview(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private String buildPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        return content.length() > 80 ? content.substring(0, 80) + "....." : content;
    }

    private ChatSession toChatSession(String uid, String sessionId, Map<String, String> meta) {
        ChatSession session = new ChatSession();
        session.setSessionId(Objects.toString(meta.get("sessionId"), sessionId));
        session.setUid(Objects.toString(meta.get("uid"), uid));
        session.setTitle(Objects.toString(meta.get("title"), DEFAULT_TITLE));
        session.setPreview(Objects.toString(meta.get("preview"), ""));
        session.setMessageCount(parseInteger(meta.get("messageCount"), 0));
        session.setStatus(Objects.toString(meta.get("status"), ChatSessionStatus.ACTIVE));
        session.setPinned(Boolean.parseBoolean(meta.get("pinned")));
        session.setPinnedAt(null);
        session.setCreatedAt(parseDateTime(meta.get("createdAt")));
        session.setUpdatedAt(parseDateTime(meta.get("updatedAt")));
        return session;
    }

    private Integer parseInteger(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private LocalDateTime parseDateTime(String epochMilli) {
        if (!StringUtils.hasText(epochMilli)) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(epochMilli)), ZoneId.systemDefault());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
