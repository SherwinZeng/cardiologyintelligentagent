package com.sherwinzeng.cardiology.cardiologygateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SentinelGatewayConfiguration {

    private static final int RATE_LIMIT_CODE = 429;

    @PostConstruct
    public void registerBlockHandler() {
        BlockRequestHandler handler = (exchange, throwable) -> {
            Map<String, Object> body = new HashMap<>();
            body.put("code", RATE_LIMIT_CODE);
            body.put("message", "当前咨询人数较多，请稍后再试");
            body.put("data", null);
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body));
        };
        GatewayCallbackManager.setBlockHandler(handler);
    }
}
