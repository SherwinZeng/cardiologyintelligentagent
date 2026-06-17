package com.sherwinzeng.cardiology.cardiologycloudcommonutils.auth;

/**
 * 网关鉴权通过后透传给下游的请求头，供 session 等服务读取，避免重复解析 JWT。
 */
public final class AuthHeaders {

    /** 当前用户 ID，与 JWT claim {@code userId} 一致 */
    public static final String USER_ID = "X-User-Id";

    /** 用户类型：{@link AuthUserType#GUEST} 或 {@link AuthUserType#FORMAL} */
    public static final String USER_TYPE = "X-User-Type";

    private AuthHeaders() {
    }
}
