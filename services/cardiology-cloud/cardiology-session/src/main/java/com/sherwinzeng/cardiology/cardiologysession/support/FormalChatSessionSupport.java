package com.sherwinzeng.cardiology.cardiologysession.support;

import com.sherwinzeng.cardiology.cardiologycloudcommondata.entity.chat.ChatSessionStatus;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.exception.ChatBusinessException;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatSessionMapper;

public final class FormalChatSessionSupport {

    private FormalChatSessionSupport() {
    }

    public static ChatSession requireOwnedSession(ChatSessionMapper chatSessionMapper, String uid, String sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "会话不存在");
        }
        if (!uid.equals(session.getUid())) {
            throw new ChatBusinessException(ResponseCode.FORBIDDEN, "无权访问该会话");
        }
        return session;
    }

    public static ChatSession requireOwnedActiveSession(ChatSessionMapper chatSessionMapper, String uid, String sessionId) {
        ChatSession session = requireOwnedSession(chatSessionMapper, uid, sessionId);
        if (ChatSessionStatus.ARCHIVED.equals(session.getStatus())) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "会话已归档，无法继续问诊");
        }
        return session;
    }
}
