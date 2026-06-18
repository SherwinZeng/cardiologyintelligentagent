package com.sherwinzeng.cardiology.cardiologysession.services.impl;

import com.rabbitmq.client.Channel;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.exception.ChatBusinessException;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.message.MessageRoundCompletedEvent;
import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.properties.CardiologyMqProperties;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatMessage;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.entity.chat.ChatMessageRole;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.entity.chat.ChatSessionStatus;
import com.sherwinzeng.cardiology.cardiologysession.feign.DRFAgentFeignClient;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatMessageMapper;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatSessionMapper;
import com.sherwinzeng.cardiology.cardiologysession.request.GeneralUnderstandingRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.response.GeneralUnderstandingResponse;
import com.sherwinzeng.cardiology.cardiologysession.services.GeneralUnderstandingService;
import com.sherwinzeng.cardiology.cardiologysession.store.GuestChatMessagePayload;
import com.sherwinzeng.cardiology.cardiologysession.store.GuestChatSessionStore;
import com.sherwinzeng.cardiology.cardiologysession.support.AuthHeaderSupport;
import com.sherwinzeng.cardiology.cardiologysession.support.FormalChatSessionSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneralUnderstandingServiceImpl implements GeneralUnderstandingService {

    /** 传给 ai-agent 的最近 message 条数（user + assistant 合计） */
    private static final int MEMORY_WINDOW_MESSAGES = 12;
    /** assistant 写入 history 时的最大字符数，避免单条长回复挤掉早期 user 消息 */
    private static final int HISTORY_ASSISTANT_MAX_CHARS = 480;

    private final DRFAgentFeignClient drfAgentFeignClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final RabbitTemplate rabbitTemplate;
    private final CardiologyMqProperties cardiologyMqProperties;
    private final GuestChatSessionStore guestChatSessionStore;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generalUnderstanding(GeneralUnderstandingRequestParams generalUnderstandingRequestParams, String userType, String authenticatedUid) throws ChatBusinessException {
        AuthHeaderSupport.assertUidMatch(generalUnderstandingRequestParams.getUid(), authenticatedUid);
        try {
            String token = UUID.randomUUID().toString();
            stringRedisTemplate.opsForValue().set("internal:token:" + token, "ok", 60, TimeUnit.SECONDS);
            ChatMessage humanMessage = null;
            if (AuthHeaderSupport.isGuest(userType)) {
                String uid = generalUnderstandingRequestParams.getUid();
                String sessionId = generalUnderstandingRequestParams.getSession();
                generalUnderstandingRequestParams.setHistory(
                        loadGuestHistory(uid, sessionId, MEMORY_WINDOW_MESSAGES)
                );
                log.info("guest history loaded | uid={} session={} turns={}",
                        uid, sessionId, generalUnderstandingRequestParams.getHistory().size());
                guestChatSessionStore.appendUserMessage(uid, sessionId, generalUnderstandingRequestParams.getMessage());
            } else {
                String uid = generalUnderstandingRequestParams.getUid();
                String sessionId = generalUnderstandingRequestParams.getSession();
                FormalChatSessionSupport.requireOwnedActiveSession(chatSessionMapper, uid, sessionId);
                generalUnderstandingRequestParams.setHistory(loadRecentHistory(uid, sessionId, MEMORY_WINDOW_MESSAGES));
                humanMessage = new ChatMessage();
                humanMessage.setSessionId(sessionId);
                humanMessage.setUid(uid);
                humanMessage.setRole(ChatMessageRole.USER);
                humanMessage.setContent(generalUnderstandingRequestParams.getMessage());
                chatMessageMapper.insert(humanMessage);
            }
            BaseResponse<GeneralUnderstandingResponse> generalUnderstandingResponseBaseResponse = drfAgentFeignClient.generalUnderstanding(token, generalUnderstandingRequestParams);
            String explanation = generalUnderstandingResponseBaseResponse.getData().getExplanation();
            if (AuthHeaderSupport.isGuest(userType)) {
                guestChatSessionStore.appendAssistantMessage(
                        generalUnderstandingRequestParams.getUid(),
                        generalUnderstandingRequestParams.getSession(),
                        explanation,
                        generalUnderstandingResponseBaseResponse.getData().getUrgency(),
                        explanation,
                        generalUnderstandingResponseBaseResponse.getData().getAdvice(),
                        generalUnderstandingResponseBaseResponse.getData().getDisclaimer()
                );
            } else {
                ChatMessage assistantMsg = new ChatMessage();
                assistantMsg.setSessionId(generalUnderstandingRequestParams.getSession());
                assistantMsg.setUid(generalUnderstandingRequestParams.getUid());
                assistantMsg.setRole(ChatMessageRole.ASSISTANT);
                assistantMsg.setContent(explanation);
                assistantMsg.setUrgency(generalUnderstandingResponseBaseResponse.getData().getUrgency());
                assistantMsg.setExplanation(explanation);
                assistantMsg.setAdvice(generalUnderstandingResponseBaseResponse.getData().getAdvice());
                assistantMsg.setDisclaimer(generalUnderstandingResponseBaseResponse.getData().getDisclaimer());
                chatMessageMapper.insert(assistantMsg);
                if (cardiologyMqProperties.isEnabled()) {
                    String preview = explanation == null || explanation.isEmpty() ? "" : explanation.length() > 80 ? explanation.substring(0, 80) + "....." : explanation;
                    MessageRoundCompletedEvent event = new MessageRoundCompletedEvent(
                            UUID.randomUUID().toString(),
                            generalUnderstandingRequestParams.getSession(),
                            generalUnderstandingRequestParams.getUid(),
                            humanMessage != null ? humanMessage.getId() : null,
                            assistantMsg.getId(),
                            preview,
                            2,
                            assistantMsg.getCreatedAt() != null ? assistantMsg.getCreatedAt() : LocalDateTime.now()
                    );
                    var sessionIndex = cardiologyMqProperties.getSessionIndex();
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            rabbitTemplate.convertAndSend(sessionIndex.getExchange(), sessionIndex.getRoutingKey(), event);
                            log.info("MQ 已发送 session-index | session={} eventId={} userMsgId={} assistantMsgId={}",
                                    event.sessionId(), event.eventId(), event.userMessageId(), event.assistantMessageId());
                        }
                    });
                }
            }
            log.info(
                    "铭铭回答了 | uid={} session={} urgency={} explanation={}",
                    generalUnderstandingRequestParams.getUid(),
                    generalUnderstandingRequestParams.getSession(),
                    generalUnderstandingResponseBaseResponse.getData().getUrgency(),
                    explanation != null && explanation.length() >= 80 ? explanation.substring(0, 80) + "....." : explanation
            );
            return JsonSerialization.toJson(generalUnderstandingResponseBaseResponse);
        } catch (ChatBusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ChatBusinessException(exception.getMessage(), exception);
        }
    }

    @RabbitListener(queues = "${cardiology.mq.session-index.queue}")
    public void onSessionIndexMessage(MessageRoundCompletedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            ChatSession session = chatSessionMapper.selectById(event.sessionId());
            if (session != null && ChatSessionStatus.ACTIVE.equals(session.getStatus())) {
                int currentCount = session.getMessageCount() == null ? 0 : session.getMessageCount();
                session.setMessageCount(currentCount + event.deltaCount());
                session.setPreview(event.preview());
                if (event.messageAt() != null) {
                    session.setUpdatedAt(event.messageAt());
                }
                chatSessionMapper.updateById(session);
            }
            channel.basicAck(deliveryTag, false);
            log.info(
                    "session-index 已更新 | session={} delta={} userMsgId={} assistantMsgId={}",
                    event.sessionId(),
                    event.deltaCount(),
                    event.userMessageId(),
                    event.assistantMessageId()
            );
        } catch (Exception exception) {
            log.error("session-index 消费失败 | session={} eventId={}", event.sessionId(), event.eventId(), exception);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private List<GeneralUnderstandingRequestParams.HistoryTurn> loadGuestHistory(String uid, String sessionId, int limit) {
        List<GuestChatMessagePayload> batch = guestChatSessionStore.listMessages(uid, sessionId, null, limit);
        if (batch.isEmpty()) {
            return Collections.emptyList();
        }
        List<GeneralUnderstandingRequestParams.HistoryTurn> history = new ArrayList<>(batch.size());
        for (GuestChatMessagePayload message : batch) {
            history.add(toHistoryTurn(message.getRole(), message.getContent(), message.getExplanation()));
        }
        return history;
    }

    private List<GeneralUnderstandingRequestParams.HistoryTurn> loadRecentHistory(String uid, String sessionId, int limit) {
        List<ChatMessage> batch = chatMessageMapper.selectRecentBySession(uid, sessionId, limit);
        if (batch.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatMessage> ordered = new ArrayList<>(batch);
        Collections.reverse(ordered);
        List<GeneralUnderstandingRequestParams.HistoryTurn> history = new ArrayList<>(ordered.size());
        for (ChatMessage message : ordered) {
            history.add(toHistoryTurn(message.getRole(), message.getContent(), message.getExplanation()));
        }
        return history;
    }

    private GeneralUnderstandingRequestParams.HistoryTurn toHistoryTurn(String role, String content, String explanation) {
        GeneralUnderstandingRequestParams.HistoryTurn turn = new GeneralUnderstandingRequestParams.HistoryTurn();
        if (ChatMessageRole.USER.equals(role)) {
            turn.setRole("user");
            turn.setContent(content);
        } else {
            turn.setRole("assistant");
            String assistantText = explanation != null && !explanation.isBlank() ? explanation : content;
            turn.setContent(truncateAssistantHistory(assistantText));
        }
        return turn;
    }

    private String truncateAssistantHistory(String content) {
        if (content == null || content.length() <= HISTORY_ASSISTANT_MAX_CHARS) {
            return content;
        }
        return content.substring(0, HISTORY_ASSISTANT_MAX_CHARS - 1) + "…";
    }
}
