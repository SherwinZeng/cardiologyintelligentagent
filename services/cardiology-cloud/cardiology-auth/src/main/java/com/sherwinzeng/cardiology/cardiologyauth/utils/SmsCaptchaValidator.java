package com.sherwinzeng.cardiology.cardiologyauth.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

public final class SmsCaptchaValidator {

    private SmsCaptchaValidator() {
    }

    public static boolean validate(
            StringRedisTemplate stringRedisTemplate,
            String captchaKeyPrefix,
            String phone,
            String captchaId,
            String captchaCode) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            return false;
        }
        String redisKey = captchaKeyPrefix + captchaId;
        String cachedValue = stringRedisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(cachedValue)) {
            return false;
        }
        String expectedValue = phone + ":" + captchaCode.trim().toLowerCase();
        if (!cachedValue.equalsIgnoreCase(expectedValue)) {
            return false;
        }
        stringRedisTemplate.delete(redisKey);
        return true;
    }
}
