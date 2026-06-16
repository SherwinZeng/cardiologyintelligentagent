package com.sherwinzeng.cardiology.cardiologysession.services;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologysession.request.CreateChatSessionRequestParams;

public interface ChatSessionService extends IService<ChatSession> {
    String createSession(CreateChatSessionRequestParams createChatSessionRequestParams);
}
