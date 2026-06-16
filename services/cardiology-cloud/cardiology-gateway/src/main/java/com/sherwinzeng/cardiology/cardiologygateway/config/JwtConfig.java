package com.sherwinzeng.cardiology.cardiologygateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * JWT 解码器配置。
 * <p>
 * cardiology-auth 使用 HS256 对称密钥签发 token，网关用同一密钥验签。
 * 若 {@code jwt.sign-key} 与 auth 服务不一致，所有 token 都会校验失败。
 * <p>
 * Nacos 配置示例：
 * <pre>
 * jwt:
 *   sign-key: change-me-to-the-same-value-as-auth-service
 * </pre>
 */
@Configuration
public class JwtConfig {

    /**
     * 注册响应式 JWT 解码器，供 {@link com.sherwinzeng.cardiology.cardiologygateway.filter.AuthenticationGlobalFilter} 使用。
     * <p>
     * 使用 ReactiveJwtDecoder 而非阻塞式 JwtDecoder，适配 Gateway 的 WebFlux 线程模型。
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(@Value("${jwt.sign-key}") String signKey) {
        byte[] keyBytes = signKey.trim().getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
