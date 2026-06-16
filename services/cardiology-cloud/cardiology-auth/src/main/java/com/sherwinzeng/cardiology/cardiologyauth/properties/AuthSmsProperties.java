package com.sherwinzeng.cardiology.cardiologyauth.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "auth.sms")
public class AuthSmsProperties {
    private String signName;
    private String templateCode;
    private Long sendInterval = 60L;
    private Long codeValidMinutes = 5L;
    private String sendLockKey = "cardiology:sms:send:lock:";
    private String codeKey = "cardiology:sms:send:code:";
    private String captchaKey = "cardiology:sms:captcha:";
    private Long captchaValidSeconds = 300L;
    private Integer captchaLength = 4;
    private String captchaChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
}
