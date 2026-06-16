package com.sherwinzeng.cardiology.cardiologyauth.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "auth.guest")
public class AuthGuestProperties {
    private String key;
    private Long time;
}