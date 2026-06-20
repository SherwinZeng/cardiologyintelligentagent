package com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.config;

import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.properties.CardiologyMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Slf4j
@RequiredArgsConstructor
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(prefix = "cardiology.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqStartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final int EXCHANGE_COUNT = 1;
    private static final int QUEUE_COUNT = 3;
    private static final int BINDING_COUNT = 3;

    private final CardiologyMqProperties mqProperties;
    private final RabbitProperties rabbitProperties;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var sessionIndex = mqProperties.getSessionIndex();
        var sessionLifecycle = mqProperties.getSessionLifecycle();
        var summarySchedule = mqProperties.getConsultationSummarySchedule();
        var brokerHost = rabbitProperties.getHost() != null ? rabbitProperties.getHost() : "localhost";
        var brokerPort = rabbitProperties.getPort() != null ? rabbitProperties.getPort() : 5672;
        var virtualHost = rabbitProperties.getVirtualHost() != null ? rabbitProperties.getVirtualHost() : "/";

        log.info("========== Cardiology RabbitMQ ==========");
        log.info("Broker      : {}:{} (vhost={})", brokerHost, brokerPort, virtualHost);
        log.info("Exchanges   : {}", EXCHANGE_COUNT);
        log.info("  - {} (topic, durable)", sessionIndex.getExchange());
        log.info("Queues      : {}", QUEUE_COUNT);
        log.info("  - {} (session-index, session 生产+消费)", sessionIndex.getQueue());
        log.info("  - {} (session-lifecycle, record 消费)", sessionLifecycle.getQueue());
        log.info("  - {} (summary-schedule, record 消费→Redis 延迟调度)", summarySchedule.getQueue());
        log.info("Bindings    : {}", BINDING_COUNT);
        log.info(
                "  - {} <-- {} [{}]",
                sessionIndex.getQueue(),
                sessionIndex.getExchange(),
                sessionIndex.getRoutingKey()
        );
        log.info(
                "  - {} <-- {} [{}]",
                sessionLifecycle.getQueue(),
                sessionLifecycle.getExchange(),
                sessionLifecycle.getRoutingKey()
        );
        log.info(
                "  - {} <-- {} [{}]",
                summarySchedule.getQueue(),
                summarySchedule.getExchange(),
                summarySchedule.getRoutingKey()
        );
        log.info("=========================================");
    }
}
