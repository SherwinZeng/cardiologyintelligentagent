package com.sherwinzeng.cardiology.cardiologyrecord.worker;

import com.rabbitmq.client.Channel;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.entity.chat.ChatSessionStatus;
import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.message.SessionLifecycleArchivedEvent;
import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.properties.CardiologyMqProperties;
import com.sherwinzeng.cardiology.cardiologyrecord.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologyrecord.properties.SessionLifecycleProperties;
import com.sherwinzeng.cardiology.cardiologyrecord.repository.ChatMessageMapper;
import com.sherwinzeng.cardiology.cardiologyrecord.repository.ChatSessionMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionLifecycleWorker {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final SessionLifecycleProperties sessionLifecycleProperties;
    private final CardiologyMqProperties cardiologyMqProperties;
    private final RabbitTemplate rabbitTemplate;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    void initTransactionTemplate() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }


    @Scheduled(cron = "${cardiology.session-lifecycle.archive-cron:0 0 2 * * ?}")
    public void archiveInactiveSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(sessionLifecycleProperties.getInactiveDays());
        List<ChatSession> sessions = chatSessionMapper.selectInactiveActiveSessions(cutoff, sessionLifecycleProperties.getArchiveBatchSize());
        if (sessions.isEmpty()) return;
        log.info("session-lifecycle 归档扫描 | candidates={} cutoff={}", sessions.size(), cutoff);
        for (ChatSession session : sessions) {
            try {
                LocalDateTime archivedAt = LocalDateTime.now();
                session.setStatus(ChatSessionStatus.ARCHIVED);
                session.setArchivedAt(archivedAt);
                chatSessionMapper.updateById(session);
                if (cardiologyMqProperties.isEnabled()) {
                    SessionLifecycleArchivedEvent event = new SessionLifecycleArchivedEvent(UUID.randomUUID().toString(), session.getSessionId(), session.getUid(), archivedAt);
                    var lifecycle = cardiologyMqProperties.getSessionLifecycle();
                    rabbitTemplate.convertAndSend(lifecycle.getExchange(), lifecycle.getRoutingKey(), event);
                    log.info(
                            "session-lifecycle MQ 已发送 | session={} uid={} eventId={}",
                            session.getSessionId(),
                            session.getUid(),
                            event.eventId()
                    );
                }
                log.info(
                        "session-lifecycle 已归档 | session={} uid={} archivedAt={}", session.getSessionId(), session.getUid(), archivedAt
                );
            } catch (Exception exception) {
                log.error("session-lifecycle 归档失败 | session={} uid={}", session.getSessionId(), session.getUid(), exception);
            }
        }
    }


    @Scheduled(cron = "${cardiology.session-lifecycle.purge-cron:0 30 2 * * ?}")
    public void purgeExpiredArchivedSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(sessionLifecycleProperties.getPurgeDays());
        List<ChatSession> sessions = chatSessionMapper.selectExpiredArchivedSessions(cutoff, sessionLifecycleProperties.getPurgeBatchSize());
        if (sessions.isEmpty()) return;

        log.info("session-lifecycle 清理扫描 | candidates={} cutoff={}", sessions.size(), cutoff);
        for (ChatSession session : sessions) {
            try {
                String sessionId = session.getSessionId();
                String uid = session.getUid();
                transactionTemplate.executeWithoutResult(status -> {
                    chatMessageMapper.deleteBySessionId(sessionId);
                    chatSessionMapper.deleteById(sessionId);
                });
                log.info("session-lifecycle 已清理 | session={} uid={}", sessionId, uid);
            } catch (Exception exception) {
                log.error("session-lifecycle 清理失败 | session={} uid={}", session.getSessionId(), session.getUid(), exception);
            }
        }
    }

    @RabbitListener(queues = "${cardiology.mq.session-lifecycle.queue}")
    public void onSessionLifecycleArchived(SessionLifecycleArchivedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("session-lifecycle 已收到 | session={} uid={} archivedAt={} eventId={}", event.sessionId(), event.uid(), event.archivedAt(), event.eventId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception exception) {
            log.error("session-lifecycle 消费失败 | session={} eventId={}", event.sessionId(), event.eventId(), exception);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
