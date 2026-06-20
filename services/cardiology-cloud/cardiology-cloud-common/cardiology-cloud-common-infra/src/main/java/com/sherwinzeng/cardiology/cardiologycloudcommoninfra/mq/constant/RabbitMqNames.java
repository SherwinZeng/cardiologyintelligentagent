package com.sherwinzeng.cardiology.cardiologycloudcommoninfra.mq.constant;

/**
 * RabbitMQ 交换机 / 队列 / 路由键默认值。
 * 可在 Nacos {@code cardiology.mq.*} 中覆盖。
 */
public final class RabbitMqNames {

    public static final String SESSION_EXCHANGE = "cardiology.session.exchange";

    public static final String SESSION_INDEX_QUEUE = "cardiology.session.index.queue";

    public static final String SESSION_INDEX_ROUTING_KEY = "session.index.updated";

    /** formal 会话生命周期（归档 / 删除），record Worker 消费 */
    public static final String SESSION_LIFECYCLE_QUEUE = "cardiology.session.lifecycle.queue";

    public static final String SESSION_LIFECYCLE_ROUTING_KEY = "session.lifecycle.archived";

    /** 问诊总结延迟调度，record Worker 消费后写入 Redis ZSET */
    public static final String CONSULTATION_SUMMARY_SCHEDULE_QUEUE = "cardiology.session.summary.schedule.queue";

    public static final String CONSULTATION_SUMMARY_SCHEDULE_ROUTING_KEY = "session.summary.schedule";

    private RabbitMqNames() {
    }
}
