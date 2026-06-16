package com.sherwinzeng.cardiology.cardiologygateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权白名单配置。
 * <p>
 * 绑定 Nacos / application.yml 中 {@code cardiology.auth} 前缀的配置项。
 * <p>
 * 配置示例：
 * <pre>
 * cardiology:
 *   auth:
 *     exclude-paths:
 *       - /auth/guest/login/**   # 游客登录
 *       - /actuator/**           # 健康检查（如有）
 * </pre>
 * <p>
 * 路径匹配使用 Ant 风格，未列入白名单的路径均需携带有效 Bearer Token。
 */
@Data
@ConfigurationProperties(prefix = "cardiology.auth")
public class AuthenticationProperties {

    /**
     * 免鉴权路径列表，默认空列表表示除白名单外全部需要鉴权。
     * 初始化为 ArrayList 避免 Nacos 未配置时出现 NPE。
     */
    private List<String> excludePaths = new ArrayList<>();
}
