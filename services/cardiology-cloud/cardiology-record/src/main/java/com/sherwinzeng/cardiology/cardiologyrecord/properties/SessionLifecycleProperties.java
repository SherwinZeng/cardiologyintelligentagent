package com.sherwinzeng.cardiology.cardiologyrecord.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * formal 会话生命周期：inactive 归档、archived 后清理。
 */
@Data
@ConfigurationProperties(prefix = "cardiology.session-lifecycle")
public class SessionLifecycleProperties {

    /** 超过该天数无活跃则归档 */
    private int inactiveDays = 15;

    /** 归档后超过该天数则物理删除会话与消息 */
    private int purgeDays = 7;

    /** 单次归档扫描上限 */
    private int archiveBatchSize = 100;

    /** 单次清理扫描上限 */
    private int purgeBatchSize = 100;

    /** 归档任务 cron，默认每天 02:00 */
    private String archiveCron = "0 0 2 * * ?";

    /** 清理任务 cron，默认每天 02:30 */
    private String purgeCron = "0 30 2 * * ?";
}
