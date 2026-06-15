package com.sherwinzeng.cardiology.cardiologyauth.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    private static final String SIGNING_KEY_ID = "cardiology-auth-signing-key";

    @Bean
    @Primary
    public JwtEncoder jwtEncoder(@Value("${jwt.sign-key}") String signKey) {
        byte[] keyBytes = signKey.trim().getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

        OctetSequenceKey jwk = new OctetSequenceKey.Builder(secretKey)
                .keyID(SIGNING_KEY_ID)
                .algorithm(JWSAlgorithm.HS256)
                .build();

        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }
}
