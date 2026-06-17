package com.sherwinzeng.cardiology.cardiologysession.support;

import com.sherwinzeng.cardiology.cardiologysession.properties.GuestChatSessionProperties;

public final class GuestChatRedisKeys {

    private GuestChatRedisKeys() {
    }

    public static String indexKey(GuestChatSessionProperties properties, String uid) {
        return properties.getKeyPrefix() + uid + ":index";
    }

    public static String metaKey(GuestChatSessionProperties properties, String uid, String sessionId) {
        return properties.getKeyPrefix() + uid + ":s:" + sessionId;
    }

    public static String msgsKey(GuestChatSessionProperties properties, String uid, String sessionId) {
        return properties.getKeyPrefix() + uid + ":s:" + sessionId + ":msgs";
    }
}
