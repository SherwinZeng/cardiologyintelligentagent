package com.sherwinzeng.cardiology.cardiologyrecord.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RecordWorkerStartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        boolean mqEnabled = env.getProperty("cardiology.mq.enabled", Boolean.class, false);
        boolean summaryEnabled = env.getProperty("cardiology.consultation-summary.enabled", Boolean.class, true);
        int idleMinutes = env.getProperty("cardiology.consultation-summary.idle-minutes", Integer.class, 60);
        int minMessages = env.getProperty("cardiology.consultation-summary.min-messages", Integer.class, 20);
        long pollIntervalMs = env.getProperty("cardiology.consultation-summary.poll-interval-ms", Long.class, 60000L);

        log.info("========== Cardiology Record Worker ==========");
        log.info("Mode        : background worker (no HTTP)");
        log.info("MQ          : {}", mqEnabled ? "enabled (lifecycle + summary-schedule)" : "disabled");
        log.info("Jobs        : session-lifecycle archive+purge / consultation-summary (Redis delay + poll)");
        if (summaryEnabled) {
            log.info(
                    "Summary     : idle-minutes={} min-messages={}(需 count>此值) poll-interval-ms={}",
                    idleMinutes,
                    minMessages,
                    pollIntervalMs
            );
        } else {
            log.info("Summary     : disabled");
        }
        log.info("==============================================");
    }
}
