package com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.properties.CardiologyMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableRabbit
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RabbitTemplate.class)
@EnableConfigurationProperties(CardiologyMqProperties.class)
@ConditionalOnProperty(prefix = "cardiology.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MessageConverter rabbitMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public TopicExchange sessionIndexExchange(CardiologyMqProperties properties) {
        return new TopicExchange(properties.getSessionIndex().getExchange(), true, false);
    }

    @Bean
    public Queue sessionIndexQueue(CardiologyMqProperties properties) {
        return new Queue(properties.getSessionIndex().getQueue(), true);
    }

    @Bean
    public Binding sessionIndexBinding(
            CardiologyMqProperties properties,
            @Qualifier("sessionIndexQueue") Queue sessionIndexQueue,
            @Qualifier("sessionIndexExchange") TopicExchange sessionIndexExchange
    ) {
        return BindingBuilder.bind(sessionIndexQueue)
                .to(sessionIndexExchange)
                .with(properties.getSessionIndex().getRoutingKey());
    }

    @Bean
    public Queue sessionLifecycleQueue(CardiologyMqProperties properties) {
        return new Queue(properties.getSessionLifecycle().getQueue(), true);
    }

    @Bean
    public Binding sessionLifecycleBinding(
            CardiologyMqProperties properties,
            @Qualifier("sessionLifecycleQueue") Queue sessionLifecycleQueue,
            @Qualifier("sessionIndexExchange") TopicExchange sessionIndexExchange
    ) {
        return BindingBuilder.bind(sessionLifecycleQueue)
                .to(sessionIndexExchange)
                .with(properties.getSessionLifecycle().getRoutingKey());
    }

    @Bean
    public RabbitMqStartupLogger rabbitMqStartupLogger(
            CardiologyMqProperties properties,
            org.springframework.boot.autoconfigure.amqp.RabbitProperties rabbitProperties
    ) {
        return new RabbitMqStartupLogger(properties, rabbitProperties);
    }
}
