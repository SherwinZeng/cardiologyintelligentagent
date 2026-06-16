package com.sherwinzeng.cardiology.cardiologysession.services.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.exception.ChatBusinessException;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatSessionStatus;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatSessionMapper;
import com.sherwinzeng.cardiology.cardiologysession.request.CreateChatSessionRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.services.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {
    private final ChatSessionMapper chatSessionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSession(CreateChatSessionRequestParams createChatSessionRequestParams) {
        String sessionId = createChatSessionRequestParams.getSession();
        String uid = createChatSessionRequestParams.getUid();

        ChatSession existing = chatSessionMapper.selectById(sessionId);
        if (existing != null) {
            if (!uid.equals(existing.getUid())) {
                throw new ChatBusinessException(ResponseCode.FORBIDDEN, "会话已被其他用户占用");
            }
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "会话已存在");
        }

        ChatSession chatSession = new ChatSession();
        chatSession.setSessionId(sessionId);
        chatSession.setUid(uid);
        chatSession.setTitle("新建会话");
        chatSession.setPreview("");
        chatSession.setMessageCount(0);
        chatSession.setStatus(ChatSessionStatus.ACTIVE);
        chatSessionMapper.insert(chatSession);
        return JsonSerialization.toJson(BaseResponse.success(chatSession));
    }
}
