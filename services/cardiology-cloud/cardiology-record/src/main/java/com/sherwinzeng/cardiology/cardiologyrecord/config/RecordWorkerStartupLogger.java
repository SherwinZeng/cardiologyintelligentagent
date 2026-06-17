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

        log.info("========== Cardiology Record Worker ==========");
        log.info("Mode        : background worker (no HTTP)");
        log.info("MQ          : {}", mqEnabled ? "enabled (session-lifecycle consumer)" : "disabled");
        log.info("Jobs        : session-lifecycle archive+purge / consultation summary (pending)");
        log.info("==============================================");
    }
}
