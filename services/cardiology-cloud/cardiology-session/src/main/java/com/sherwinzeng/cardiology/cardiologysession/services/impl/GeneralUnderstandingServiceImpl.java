package com.sherwinzeng.cardiology.cardiologysession.services.impl;

import com.rabbitmq.client.Channel;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.exception.ChatBusinessException;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.message.ConsultationSummaryScheduleEvent;
import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.message.MessageRoundCompletedEvent;
import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.properties.CardiologyMqProperties;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatMessage;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.entity.chat.ChatMessageRole;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.entity.chat.ChatSessionStatus;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.entity.chat.ChatSessionSummaryStatus;
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
import com.sherwinzeng.cardiology.cardiologysession.support.GuideReferenceSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneralUnderstandingServiceImpl implements GeneralUnderstandingService {

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
                guestChatSessionStore.appendUserMessage(
                        generalUnderstandingRequestParams.getUid(),
                        generalUnderstandingRequestParams.getSession(),
                        generalUnderstandingRequestParams.getMessage()
                );
            } else {
                String uid = generalUnderstandingRequestParams.getUid();
                String sessionId = generalUnderstandingRequestParams.getSession();
                FormalChatSessionSupport.requireOwnedActiveSession(chatSessionMapper, uid, sessionId);
                humanMessage = new ChatMessage();
                humanMessage.setSessionId(sessionId);
                humanMessage.setUid(uid);
                humanMessage.setRole(ChatMessageRole.USER);
                humanMessage.setContent(generalUnderstandingRequestParams.getMessage());
                chatMessageMapper.insert(humanMessage);
            }
            BaseResponse<GeneralUnderstandingResponse> generalUnderstandingResponseBaseResponse =
                    drfAgentFeignClient.generalUnderstanding(token, generalUnderstandingRequestParams);
            if (generalUnderstandingResponseBaseResponse == null
                    || generalUnderstandingResponseBaseResponse.getCode() != ResponseCode.SUCCESS
                    || generalUnderstandingResponseBaseResponse.getData() == null) {
                String message = generalUnderstandingResponseBaseResponse != null
                        && generalUnderstandingResponseBaseResponse.getMessage() != null
                        ? generalUnderstandingResponseBaseResponse.getMessage()
                        : "铭铭暂时繁忙，请稍后再试";
                throw new ChatBusinessException(ResponseCode.SERVICE_UNAVAILABLE, message);
            }
            String explanation = generalUnderstandingResponseBaseResponse.getData().getExplanation();
            if (AuthHeaderSupport.isGuest(userType)) {
                guestChatSessionStore.appendAssistantMessage(
                        generalUnderstandingRequestParams.getUid(),
                        generalUnderstandingRequestParams.getSession(),
                        explanation,
                        generalUnderstandingResponseBaseResponse.getData().getUrgency(),
                        explanation,
                        generalUnderstandingResponseBaseResponse.getData().getAdvice(),
                        generalUnderstandingResponseBaseResponse.getData().getDisclaimer(),
                        generalUnderstandingResponseBaseResponse.getData().getGuideReferences()
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
                assistantMsg.setGuideReferences(
                        GuideReferenceSupport.encode(
                                generalUnderstandingResponseBaseResponse.getData().getGuideReferences()
                        )
                );
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
                    var summarySchedule = cardiologyMqProperties.getConsultationSummarySchedule();
                    LocalDateTime messageAt = assistantMsg.getCreatedAt() != null ? assistantMsg.getCreatedAt() : LocalDateTime.now();
                    ConsultationSummaryScheduleEvent scheduleEvent = new ConsultationSummaryScheduleEvent(UUID.randomUUID().toString(), generalUnderstandingRequestParams.getSession(),
                            generalUnderstandingRequestParams.getUid(), messageAt
                    );
                    registerMqAfterCommit(event, scheduleEvent, sessionIndex, summarySchedule);
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

    private void registerMqAfterCommit(MessageRoundCompletedEvent indexEvent, ConsultationSummaryScheduleEvent scheduleEvent, CardiologyMqProperties.SessionIndex sessionIndex,
                                       CardiologyMqProperties.ConsultationSummarySchedule summarySchedule) {
        TransactionSynchronizationManager.registerSynchronization(
                new AfterCommitMqSynchronization(rabbitTemplate, indexEvent, scheduleEvent, sessionIndex, summarySchedule)
        );
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
                session.setSummaryStatus(ChatSessionSummaryStatus.PENDING);
                session.setSummaryGeneratedAt(null);
                session.setSummaryAttemptedAt(null);
                session.setSummaryError(null);
                session.setSummaryRetryCount(0);
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

    /**
     * 事务提交后发送 MQ，使用显式静态内部类避免匿名类与外层类增量编译字节码不一致。
     */
    private static final class AfterCommitMqSynchronization implements TransactionSynchronization {

        private static final Logger log = LoggerFactory.getLogger(AfterCommitMqSynchronization.class);

        private final RabbitTemplate rabbitTemplate;
        private final MessageRoundCompletedEvent indexEvent;
        private final ConsultationSummaryScheduleEvent scheduleEvent;
        private final CardiologyMqProperties.SessionIndex sessionIndex;
        private final CardiologyMqProperties.ConsultationSummarySchedule summarySchedule;

        private AfterCommitMqSynchronization(
                RabbitTemplate rabbitTemplate,
                MessageRoundCompletedEvent indexEvent,
                ConsultationSummaryScheduleEvent scheduleEvent,
                CardiologyMqProperties.SessionIndex sessionIndex,
                CardiologyMqProperties.ConsultationSummarySchedule summarySchedule
        ) {
            this.rabbitTemplate = rabbitTemplate;
            this.indexEvent = indexEvent;
            this.scheduleEvent = scheduleEvent;
            this.sessionIndex = sessionIndex;
            this.summarySchedule = summarySchedule;
        }

        @Override
        public void afterCommit() {
            rabbitTemplate.convertAndSend(sessionIndex.getExchange(), sessionIndex.getRoutingKey(), indexEvent);
            log.info(
                    "MQ 已发送 session-index | session={} eventId={} userMsgId={} assistantMsgId={}",
                    indexEvent.sessionId(),
                    indexEvent.eventId(),
                    indexEvent.userMessageId(),
                    indexEvent.assistantMessageId()
            );

            rabbitTemplate.convertAndSend(
                    summarySchedule.getExchange(),
                    summarySchedule.getRoutingKey(),
                    scheduleEvent
            );
            log.info(
                    "MQ 已发送 summary-schedule | session={} eventId={} messageAt={}",
                    scheduleEvent.sessionId(),
                    scheduleEvent.eventId(),
                    scheduleEvent.messageAt()
            );
        }
    }
}
