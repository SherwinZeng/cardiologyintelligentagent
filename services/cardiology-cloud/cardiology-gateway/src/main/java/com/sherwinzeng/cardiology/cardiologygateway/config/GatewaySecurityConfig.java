package com.sherwinzeng.cardiology.cardiologygateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security WebFlux 配置。
 * <p>
 * 引入 {@code spring-boot-starter-oauth2-resource-server} 后，Spring Security 会默认启用
 * OAuth2 资源服务器过滤器链，可能和自定义的 {@code AuthenticationGlobalFilter} 冲突。
 * <p>
 * 这里显式关闭 Security 层的鉴权拦截，所有鉴权逻辑由网关 GlobalFilter 统一处理。
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // 网关 API 无 CSRF 场景，关闭 CSRF
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // 放行所有请求，鉴权交给 AuthenticationGlobalFilter
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }
}
