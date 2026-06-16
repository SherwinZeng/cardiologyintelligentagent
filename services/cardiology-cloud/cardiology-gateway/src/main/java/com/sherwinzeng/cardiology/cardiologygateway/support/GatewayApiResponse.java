package com.sherwinzeng.cardiology.cardiologygateway.support;

import lombok.Data;

/**
 * 网关统一 API 响应体。
 * <p>
 * 字段结构与 cardiology-cloud-common-data 中的 {@code BaseResponse} 对齐，
 * 但网关不依赖 common-data 模块（避免引入 spring-boot-starter-web / Tomcat 与 WebFlux 冲突）。
 * <p>
 * 前端处理示例：
 * <pre>
 * if (response.code === 403) {
 *   // 跳转登录页或提示 response.message
 * }
 * </pre>
 */
@Data
public class GatewayApiResponse<T> {

    /** 业务状态码，403 表示未授权/登录失效 */
    private Integer code;

    /** 提示信息，直接展示给用户 */
    private String message;

    /** 业务数据，鉴权失败时为 null */
    private T data;

    public static <T> GatewayApiResponse<T> fail(int code, String message) {
        GatewayApiResponse<T> response = new GatewayApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(null);
        return response;
    }
}
