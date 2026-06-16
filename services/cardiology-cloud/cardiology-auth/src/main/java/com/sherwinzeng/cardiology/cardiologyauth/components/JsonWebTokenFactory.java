package com.sherwinzeng.cardiology.cardiologyauth.components;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RefreshScope
@RequiredArgsConstructor
public class JsonWebTokenFactory {
    private final JwtEncoder jwtEncoder;

    @Value("${jwt.token-validity:7200}")
    private Long validity;

    public String generateToken(String userId) {
        return generateToken(userId, null, validity);
    }

    public String generateToken(String userId, Long time) {
        return generateToken(userId, null, time);
    }

    /**
     * @param userId        用户 ID
     * @param userType      用户类型，见 {@link com.sherwinzeng.cardiology.cardiologycloudcommonutils.auth.AuthUserType}
     * @param expireSeconds 过期秒数，null 时使用默认配置
     */
    public String generateToken(String userId, String userType, Long expireSeconds) {
        Instant now = Instant.now();
        long seconds = expireSeconds != null ? expireSeconds : validity;
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .subject(userId)
                .claim("userId", userId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(seconds));
        if (userType != null && !userType.isBlank()) {
            builder.claim(com.sherwinzeng.cardiology.cardiologycloudcommonutils.auth.AuthUserType.CLAIM_NAME, userType);
        }
        return encode(builder.build());
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }
}
