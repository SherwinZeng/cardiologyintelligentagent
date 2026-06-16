package com.sherwinzeng.cardiology.cardiologycloudcommonutils.auth;

/**
 * JWT 中的用户类型标识，供 auth 签发与 gateway 鉴权分支使用。
 */
public final class AuthUserType {

    /** JWT claim 字段名 */
    public static final String CLAIM_NAME = "userType";

    /** 游客：网关需校验 JWT + Redis 会话 */
    public static final String GUEST = "guest";

    /** 正式用户：网关仅校验 JWT 签名与过期时间 */
    public static final String FORMAL = "formal";

    private AuthUserType() {
    }
}
