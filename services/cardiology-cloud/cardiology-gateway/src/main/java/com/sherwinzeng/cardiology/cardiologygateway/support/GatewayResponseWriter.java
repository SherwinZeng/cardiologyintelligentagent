package com.sherwinzeng.cardiology.cardiologygateway.support;

import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关统一响应写入器（WebFlux 响应式）。
 * <p>
 * 网关是响应式栈，不能像 MVC 的 {@code @RestControllerAdvice} 那样抛异常后自动转 JSON，
 * 需要在 Filter 里主动写入响应体。
 * <p>
 * 响应格式与业务微服务的 {@code BaseResponse} 保持一致，前端可统一按 code / message 处理：
 * <pre>
 * {
 *   "code": 403,
 *   "message": "请先登录",
 *   "data": null
 * }
 * </pre>
 */
@Component
public class GatewayResponseWriter {

    /** 与业务服务 ResponseCode.FORBIDDEN 保持一致 */
    private static final int FORBIDDEN_CODE = 403;

    /**
     * 写入鉴权失败响应。
     *
     * @param exchange 当前请求上下文
     * @param message  返回给前端的提示文案
     * @return 写完响应后的 Mono
     */
    public Mono<Void> writeForbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();

        // 响应已提交（例如下游已返回 200）时不能再改 header/body，直接结束
        if (response.isCommitted()) {
            return Mono.empty();
        }

        String body = JsonSerialization.toJson(GatewayApiResponse.fail(FORBIDDEN_CODE, message));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        response.setStatusCode(HttpStatus.FORBIDDEN);
        // 使用 set(key, value) 写入，避免对已提交的 ReadOnlyHttpHeaders 调用 setContentType 抛异常
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
