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
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .claim("userId", userId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(validity))
                .build();
        return encode(claims);
    }

    public String generateToken(String userId, Long time) {
        Instant now = Instant.now();
        long expireSeconds = time != null ? time : validity;
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .claim("userId", userId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expireSeconds))
                .build();
        return encode(claims);
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }
}
