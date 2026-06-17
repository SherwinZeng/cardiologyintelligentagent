package com.sherwinzeng.cardiology.cardiologygateway.filter;

import com.sherwinzeng.cardiology.cardiologygateway.properties.AuthGuestProperties;
import com.sherwinzeng.cardiology.cardiologygateway.properties.AuthenticationProperties;
import com.sherwinzeng.cardiology.cardiologygateway.support.GatewayResponseWriter;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.auth.AuthHeaders;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.auth.AuthUserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关全局鉴权过滤器。
 * <p>
 * 职责：在请求进入下游微服务之前，按用户类型完成鉴权，并将 {@code userId}、{@code userType}
 * 写入请求头透传给下游，避免各微服务重复解析 JWT。
 * <p>
 * 校验策略（由 JWT claim {@link AuthUserType#CLAIM_NAME} 决定）：
 * <ul>
 *   <li>{@link AuthUserType#GUEST} 游客：JWT 校验 + Redis 会话校验（单点登录、踢下线）</li>
 *   <li>{@link AuthUserType#FORMAL} 正式用户：仅 JWT 校验（签名 + 过期时间）</li>
 *   <li>未携带 userType 的旧 token：按游客处理，透传 {@code guest}，需重新登录以获取带类型的 token</li>
 * </ul>
 * <p>
 * 鉴权通过后写入下游请求头（常量见 {@link AuthHeaders}）：
 * <ul>
 *   <li>{@link AuthHeaders#USER_ID}：JWT 中的 {@code userId}</li>
 *   <li>{@link AuthHeaders#USER_TYPE}：{@code guest} 或 {@code formal}，与鉴权分支一致</li>
 * </ul>
 * 下游示例：{@code request.getHeader(AuthHeaders.USER_TYPE)}
 * <p>
 * 鉴权失败时直接返回统一 JSON（HTTP 403，body 格式与业务服务 {@code BaseResponse} 一致），
 * 不抛异常、不走下游，方便前端统一处理。
 * <p>
 * Nacos 相关配置：
 * <pre>
 * jwt:
 *   sign-key: xxx                        # 与 auth 服务保持一致
 * auth:
 *   guest:
 *     key: "cardiology:guest:session:"   # Redis 会话 key 前缀，注意 YAML 中需加引号
 * cardiology:
 *   auth:
 *     exclude-paths:                     # 白名单，支持 Ant 风格
 *       - /auth/guest/login/**
 * </pre>
 * <p>
 * 注意：{@code chain.filter()} 返回的是 {@code Mono<Void>}（无元素流），
 * 不能在它后面接 {@code switchIfEmpty}，否则下游正常返回后会被误判为“空流”再次写 403。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationGlobalFilter implements GlobalFilter, Ordered {

    /** Authorization 请求头中 Bearer 前缀，格式：Bearer &lt;token&gt; */
    private static final String BEARER_PREFIX = "Bearer ";

    /** 白名单路径配置，对应 Nacos：cardiology.auth.exclude-paths */
    private final AuthenticationProperties authenticationProperties;

    /** 游客会话 Redis 配置，对应 Nacos：auth.guest.key / auth.guest.time */
    private final AuthGuestProperties authGuestProperties;

    /** 响应式 JWT 解码器，由 {@link com.sherwinzeng.cardiology.cardiologygateway.config.JwtConfig} 提供 */
    private final ReactiveJwtDecoder reactiveJwtDecoder;

    /** 响应式 Redis 客户端，用于校验游客会话是否仍有效 */
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    /** 鉴权失败时写统一 JSON 响应 */
    private final GatewayResponseWriter gatewayResponseWriter;

    /** Ant 风格路径匹配器，用于白名单判断（支持 * 和 **） */
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // CORS 预检请求不带 token，必须放行，否则浏览器跨域请求会被拦截
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            log.debug("OPTIONS 预检请求，跳过鉴权，path={}", path);
            return chain.filter(exchange);
        }

        // 登录等白名单接口不需要鉴权
        if (isExclude(path)) {
            log.debug("白名单路径，跳过鉴权，path={}，method={}", path, method);
            return chain.filter(exchange);
        }

        // 从请求头提取 Bearer Token
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            log.warn("鉴权失败：缺少或无效的 Authorization 请求头，path={}，method={}", path, method);
            return forbidden(exchange, "请先登录");
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        log.debug("开始鉴权，path={}，method={}", path, method);

        // 第一步：JWT 签名校验 + 过期时间校验
        return reactiveJwtDecoder.decode(token)
                .flatMap(jwt -> validateAndForward(exchange, chain, request, token, jwt, path, method))
                .onErrorResume(JwtException.class, ex -> {
                    log.warn("鉴权失败：JWT 解析失败，path={}，method={}，原因={}", path, method, ex.getMessage());
                    return forbidden(exchange, "登录已过期，请重新登录");
                });
    }

    /**
     * JWT 解码通过后，按 userType 分支校验并转发。
     * <p>
     * 正式用户（{@link AuthUserType#FORMAL}）只信任 JWT，不查 Redis，透传 {@code formal}；
     * 游客（{@link AuthUserType#GUEST}）或未带类型的旧 token，额外校验 Redis 会话，透传 {@code guest}。
     */
    private Mono<Void> validateAndForward(ServerWebExchange exchange,
                                          GatewayFilterChain chain,
                                          ServerHttpRequest request,
                                          String token,
                                          Jwt jwt,
                                          String path,
                                          String method) {
        String userId = jwt.getClaimAsString("userId");
        if (!StringUtils.hasText(userId)) {
            log.warn("鉴权失败：JWT 中缺少 userId，path={}", path);
            return forbidden(exchange, "登录信息无效，请重新登录");
        }

        String userType = jwt.getClaimAsString(AuthUserType.CLAIM_NAME);
        if (AuthUserType.FORMAL.equals(userType)) {
            log.debug("正式用户，仅校验 JWT，userId={}，path={}", userId, path);
            return forward(exchange, chain, request, userId, AuthUserType.FORMAL, path, method);
        }

        log.debug("游客用户，校验 JWT + Redis 会话，userId={}，userType={}，path={}", userId, userType, path);
        return validateGuestSessionAndForward(exchange, chain, request, token, userId, path, method);
    }

    /**
     * 游客 Redis 会话校验，通过后透传 {@link AuthHeaders#USER_TYPE} = {@link AuthUserType#GUEST}。
     * <p>
     * Redis key 规则与 cardiology-auth 的 {@code GuestLoginServiceImpl} 一致：
     * {@code auth.guest.key + userId}。
     */
    private Mono<Void> validateGuestSessionAndForward(ServerWebExchange exchange,
                                                      GatewayFilterChain chain,
                                                      ServerHttpRequest request,
                                                      String token,
                                                      String userId,
                                                      String path,
                                                      String method) {
        if (!StringUtils.hasText(authGuestProperties.getKey())) {
            log.error("鉴权失败：未配置 auth.guest.key，userId={}，path={}", userId, path);
            return forbidden(exchange, "鉴权服务配置异常，请稍后重试");
        }

        String sessionKey = authGuestProperties.getKey() + userId;
        log.debug("校验游客会话，userId={}，sessionKey={}，path={}", userId, sessionKey, path);

        return reactiveStringRedisTemplate.opsForValue().get(sessionKey)
                .defaultIfEmpty("")
                .flatMap(cachedToken -> {
                    if (!StringUtils.hasText(cachedToken)) {
                        log.warn("鉴权失败：Redis 中未找到会话，userId={}，sessionKey={}，path={}", userId, sessionKey, path);
                        return forbidden(exchange, "登录状态已失效，请重新登录");
                    }
                    if (!token.equals(cachedToken)) {
                        log.warn("鉴权失败：Redis 会话 token 不匹配，userId={}，path={}", userId, path);
                        return forbidden(exchange, "账号已在其他设备登录，请重新登录");
                    }
                    return forward(exchange, chain, request, userId, AuthUserType.GUEST, path, method);
                });
    }

    /**
     * 鉴权通过，写入 {@link AuthHeaders#USER_ID}、{@link AuthHeaders#USER_TYPE} 并转发下游。
     *
     * @param userType {@link AuthUserType#GUEST} 或 {@link AuthUserType#FORMAL}
     */
    private Mono<Void> forward(ServerWebExchange exchange,
                               GatewayFilterChain chain,
                               ServerHttpRequest request,
                               String userId,
                               String userType,
                               String path,
                               String method) {
        log.info("鉴权通过，userId={}，userType={}，path={}，method={}", userId, userType, path, method);
        ServerHttpRequest mutated = request.mutate()
                .header(AuthHeaders.USER_ID, userId)
                .header(AuthHeaders.USER_TYPE, userType)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /** 返回鉴权失败的统一 JSON 响应，HTTP 403 */
    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        log.debug("返回鉴权失败响应，message={}", message);
        return gatewayResponseWriter.writeForbidden(exchange, message);
    }

    /**
     * 过滤器执行顺序，值越小越先执行。
     * <p>
     * 设为 -100 是为了在路由转发、Sentinel 限流等过滤器之前完成鉴权，
     * 避免无效请求打到下游服务。
     */
    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 判断请求路径是否在白名单中。
     * <p>
     * 白名单路径支持 Ant 风格：
     * <ul>
     *   <li>{@code *} 匹配单层路径，例如 /auth/&#42;/login</li>
     *   <li>{@code **} 匹配多层路径，例如 /auth/guest/login/&#42;&#42;</li>
     * </ul>
     */
    private boolean isExclude(String path) {
        List<String> excludePaths = authenticationProperties.getExcludePaths();
        if (excludePaths == null || excludePaths.isEmpty()) {
            return false;
        }
        return excludePaths.stream().anyMatch(pattern -> antPathMatcher.match(pattern, path));
    }
}
