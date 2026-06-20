package com.sherwinzeng.cardiology.cardiologyrecord.worker;

import com.rabbitmq.client.Channel;
import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.message.ConsultationSummaryScheduleEvent;
import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.redis.ConsultationSummaryScheduleStore;
import com.sherwinzeng.cardiology.cardiologyrecord.properties.ConsultationSummaryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 消费「总结调度」MQ：只负责写入 Redis 延迟队列，不扫 MySQL、不调 AI。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cardiology.consultation-summary", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConsultationSummaryScheduleListener {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ConsultationSummaryScheduleStore scheduleStore;
    private final ConsultationSummaryProperties consultationSummaryProperties;

    @RabbitListener(queues = "${cardiology.mq.consultation-summary-schedule.queue}")
    public void onSummarySchedule(ConsultationSummaryScheduleEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            LocalDateTime messageAt = event.messageAt() != null ? event.messageAt() : LocalDateTime.now();
            long executeAt = messageAt.plusMinutes(consultationSummaryProperties.getIdleMinutes()).atZone(ZONE).toInstant().toEpochMilli();
            scheduleStore.schedule(event.sessionId(), event.uid(), executeAt);
            log.info(
                    "consultation-summary 已入延迟调度 | session={} uid={} executeAt={} idleMinutes={}",
                    event.sessionId(),
                    event.uid(),
                    executeAt,
                    consultationSummaryProperties.getIdleMinutes()
            );
            channel.basicAck(deliveryTag, false);
        } catch (Exception exception) {
            log.error(
                    "consultation-summary 调度失败 | session={} eventId={}",
                    event.sessionId(),
                    event.eventId(),
                    exception
            );
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
