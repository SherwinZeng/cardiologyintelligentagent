package com.sherwinzeng.cardiology.cardiologygateway;

import com.sherwinzeng.cardiology.cardiologygateway.properties.AuthGuestProperties;
import com.sherwinzeng.cardiology.cardiologygateway.properties.AuthenticationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 心血管智能问诊 - API 网关启动类。
 * <p>
 * 网关职责：
 * <ul>
 *   <li>统一入口：对外暴露 30000 端口，按路径路由到 auth / session 等微服务</li>
 *   <li>统一鉴权：JWT + Redis 会话双层校验（见 {@code AuthenticationGlobalFilter}）</li>
 *   <li>服务发现：通过 Nacos 发现下游服务实例（lb://服务名）</li>
 * </ul>
 * <p>
 * 配置加载顺序：bootstrap.yml → Nacos {@code cardiology-gateway-server.yaml} → application.yml（本地兜底）
 * <p>
 * 注意：必须使用 WebFlux 模式（bootstrap.yml 中 web-application-type: reactive），
 * 不要引入 spring-boot-starter-web，否则会出现 Tomcat 与 Netty 共存问题。
 */
@EnableDiscoveryClient
@EnableConfigurationProperties({AuthenticationProperties.class, AuthGuestProperties.class})
@SpringBootApplication
public class CardiologyGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardiologyGatewayApplication.class, args);
    }
}
