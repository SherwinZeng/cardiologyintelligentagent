package com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.properties;

import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.constant.RabbitMqNames;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cardiology.mq")
public class CardiologyMqProperties {

    private boolean enabled = true;

    private SessionIndex sessionIndex = new SessionIndex();

    private SessionLifecycle sessionLifecycle = new SessionLifecycle();

    private ConsultationSummarySchedule consultationSummarySchedule = new ConsultationSummarySchedule();

    @Data
    public static class SessionIndex {

        private String exchange = RabbitMqNames.SESSION_EXCHANGE;

        private String queue = RabbitMqNames.SESSION_INDEX_QUEUE;

        private String routingKey = RabbitMqNames.SESSION_INDEX_ROUTING_KEY;
    }

    @Data
    public static class SessionLifecycle {

        private String exchange = RabbitMqNames.SESSION_EXCHANGE;

        private String queue = RabbitMqNames.SESSION_LIFECYCLE_QUEUE;

        private String routingKey = RabbitMqNames.SESSION_LIFECYCLE_ROUTING_KEY;
    }

    @Data
    public static class ConsultationSummarySchedule {

        private String exchange = RabbitMqNames.SESSION_EXCHANGE;

        private String queue = RabbitMqNames.CONSULTATION_SUMMARY_SCHEDULE_QUEUE;

        private String routingKey = RabbitMqNames.CONSULTATION_SUMMARY_SCHEDULE_ROUTING_KEY;
    }
}
