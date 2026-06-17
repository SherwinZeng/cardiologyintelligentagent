package com.sherwinzeng.cardiology.cardiologysession.services;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologysession.request.CreateChatSessionRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.request.PinChatSessionRequestParams;

public interface ChatSessionService extends IService<ChatSession> {
    String createSession(CreateChatSessionRequestParams createChatSessionRequestParams,
                         String userType,
                         String authenticatedUid);

    String listSessions(String uid, Integer page, Integer pageSize, String keyword,
                        String userType,
                        String authenticatedUid);

    String deleteSession(String uid, String sessionId, String userType, String authenticatedUid);

    String pinSession(PinChatSessionRequestParams params, String userType, String authenticatedUid);
}
