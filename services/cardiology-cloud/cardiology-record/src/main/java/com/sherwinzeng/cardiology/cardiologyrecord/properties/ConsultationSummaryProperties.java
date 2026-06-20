package com.sherwinzeng.cardiology.cardiologyrecord.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "cardiology.consultation-summary")
public class ConsultationSummaryProperties {

    private boolean enabled = true;
    private int idleMinutes = 60;
    private int minMessages = 20;
    private int processBatchSize = 20;
    private int maxMessages = 120;
    private int maxRetryCount = 3;
    private long pollIntervalMs = 60000L;
}
