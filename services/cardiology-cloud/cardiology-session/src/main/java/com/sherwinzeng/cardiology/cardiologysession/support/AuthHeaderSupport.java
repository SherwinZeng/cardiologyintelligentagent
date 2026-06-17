package com.sherwinzeng.cardiology.cardiologysession.support;

import com.sherwinzeng.cardiology.cardiologycloudcommondata.exception.ChatBusinessException;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.auth.AuthUserType;
import org.springframework.util.StringUtils;

public final class AuthHeaderSupport {

    private AuthHeaderSupport() {
    }

    public static boolean isGuest(String userType) {
        return AuthUserType.GUEST.equals(userType);
    }

    public static void assertUidMatch(String bodyUid, String headerUid) {
        if (StringUtils.hasText(headerUid) && !headerUid.equals(bodyUid)) {
            throw new ChatBusinessException(ResponseCode.FORBIDDEN, "用户信息不一致");
        }
    }
}
