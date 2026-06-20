package com.sherwinzeng.cardiology.cardiologyrecord.services.impl;

import com.sherwinzeng.cardiology.cardiologycloudcommondata.exception.ChatBusinessException;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.redis.ConsultationSummaryScheduleStore;
import com.sherwinzeng.cardiology.cardiologyrecord.entity.ChatMessage;
import com.sherwinzeng.cardiology.cardiologyrecord.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologyrecord.feign.RecordAgentFeignClient;
import com.sherwinzeng.cardiology.cardiologyrecord.properties.ConsultationSummaryProperties;
import com.sherwinzeng.cardiology.cardiologyrecord.repository.ChatMessageMapper;
import com.sherwinzeng.cardiology.cardiologyrecord.repository.ChatSessionMapper;
import com.sherwinzeng.cardiology.cardiologyrecord.request.SessionSummaryRequestParams;
import com.sherwinzeng.cardiology.cardiologyrecord.response.SessionSummaryResponse;
import com.sherwinzeng.cardiology.cardiologyrecord.services.ConsultationRecordService;
import com.sherwinzeng.cardiology.cardiologyrecord.services.ConsultationSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 问诊总结调度：Redis 延迟触发 + 调 Python 生成，落库委托 {@link ConsultationRecordServiceImpl}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationSummaryServiceImpl implements ConsultationSummaryService {

    private static final int INTERNAL_TOKEN_TTL_SECONDS = 60;
    private static final int STUCK_PROCESSING_MINUTES = 30;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final RecordAgentFeignClient recordAgentFeignClient;
    private final ConsultationSummaryProperties consultationSummaryProperties;
    private final ConsultationSummaryScheduleStore scheduleStore;
    private final StringRedisTemplate stringRedisTemplate;
    private final ConsultationRecordService consultationRecordService;

    @Override
    @Scheduled(fixedDelayString = "${cardiology.consultation-summary.poll-interval-ms:60000}")
    public void pollDueSummaries() {
        if (!consultationSummaryProperties.isEnabled()) {
            return;
        }
        recoverStuckProcessing();
        long scheduledCount = scheduleStore.countScheduled();
        List<String> dueSessionIds = scheduleStore.pollDueSessionIds(consultationSummaryProperties.getProcessBatchSize());
        if (dueSessionIds.isEmpty()) {
            if (scheduledCount > 0) {
                log.info(
                        "consultation-summary 轮询 | due=0 scheduled={} idleMinutes={} (尚未到期，需停止聊天后等待)",
                        scheduledCount,
                        consultationSummaryProperties.getIdleMinutes()
                );
            }
            return;
        }
        log.info("consultation-summary 到期执行 | count={}", dueSessionIds.size());
        for (String sessionId : dueSessionIds) {
            try {
                processDueSession(sessionId);
            } finally {
                scheduleStore.clearUid(sessionId);
            }
        }
    }

    private void recoverStuckProcessing() {
        LocalDateTime stuckBefore = LocalDateTime.now().minusMinutes(STUCK_PROCESSING_MINUTES);
        int recovered = chatSessionMapper.recoverStuckSummaryProcessing(stuckBefore);
        if (recovered > 0) {
            log.warn("consultation-summary 恢复卡住 processing | count={} stuckBefore={}", recovered, stuckBefore);
        }
    }

    private void processDueSession(String sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            log.info("consultation-summary 跳过 | session={} reason=会话不存在", sessionId);
            return;
        }
        if (!"active".equals(session.getStatus())) {
            log.info("consultation-summary 跳过 | session={} reason=非 active", sessionId);
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(consultationSummaryProperties.getIdleMinutes());
        if (session.getUpdatedAt() != null && session.getUpdatedAt().isAfter(cutoff)) {
            log.info("consultation-summary 跳过活跃会话 | session={} uid={}", sessionId, session.getUid());
            reschedule(session, cutoff);
            return;
        }
        int messageCount = session.getMessageCount() == null ? 0 : session.getMessageCount();
        if (messageCount <= consultationSummaryProperties.getMinMessages()) {
            log.info(
                    "consultation-summary 跳过 | session={} reason=消息不足 count={} need>{}",
                    sessionId,
                    messageCount,
                    consultationSummaryProperties.getMinMessages()
            );
            return;
        }

        int locked = chatSessionMapper.markSummaryProcessing(
                sessionId,
                cutoff,
                consultationSummaryProperties.getMinMessages(),
                LocalDateTime.now()
        );
        if (locked == 0) {
            log.info("consultation-summary 跳过 | session={} reason=状态未锁定", sessionId);
            return;
        }

        try {
            List<ChatMessage> messages = chatMessageMapper.selectSessionMessages(
                    session.getUid(),
                    sessionId,
                    consultationSummaryProperties.getMaxMessages()
            );
            SessionSummaryResponse summary = requestAiSummary(session, messages);
            consultationRecordService.persistSummary(session, summary);
            log.info(
                    "consultation-summary 已保存 | session={} uid={} title={} urgency={}",
                    sessionId,
                    session.getUid(),
                    summary.getTitle(),
                    summary.getUrgency()
            );
        } catch (Exception exception) {
            String error = truncate(exception.getMessage(), 500);
            chatSessionMapper.markSummaryFailed(sessionId, error, LocalDateTime.now());
            log.warn("consultation-summary 失败 | session={} uid={} err={}", sessionId, session.getUid(), error);
        }
    }

    private void reschedule(ChatSession session, LocalDateTime cutoff) {
        long executeAt = session.getUpdatedAt() != null
                ? session.getUpdatedAt()
                .plusMinutes(consultationSummaryProperties.getIdleMinutes())
                .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                .toInstant()
                .toEpochMilli()
                : System.currentTimeMillis();
        scheduleStore.schedule(session.getSessionId(), session.getUid(), executeAt);
    }

    private SessionSummaryResponse requestAiSummary(ChatSession session, List<ChatMessage> messages) {
        String token = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set("internal:token:" + token, "ok", INTERNAL_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        SessionSummaryRequestParams params = new SessionSummaryRequestParams();
        params.setUid(session.getUid());
        params.setSession(session.getSessionId());
        params.setMessageCount(session.getMessageCount());
        params.setMessages(toMessageTurns(messages));

        BaseResponse<SessionSummaryResponse> response = recordAgentFeignClient.summarizeSession(token, params);
        if (response == null || response.getCode() == null || response.getCode() != ResponseCode.SUCCESS || response.getData() == null) {
            throw new ChatBusinessException(ResponseCode.SERVER_ERROR, "AI 总结接口返回异常");
        }
        return response.getData();
    }

    private List<SessionSummaryRequestParams.MessageTurn> toMessageTurns(List<ChatMessage> messages) {
        List<SessionSummaryRequestParams.MessageTurn> turns = new ArrayList<>();
        for (ChatMessage message : messages) {
            SessionSummaryRequestParams.MessageTurn turn = new SessionSummaryRequestParams.MessageTurn();
            turn.setRole(message.getRole());
            turn.setContent(message.getContent());
            turn.setUrgency(message.getUrgency());
            turn.setAdvice(message.getAdvice());
            turn.setDisclaimer(message.getDisclaimer());
            turn.setCreatedAt(message.getCreatedAt() == null ? "" : message.getCreatedAt().toString());
            turns.add(turn);
        }
        return turns;
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
