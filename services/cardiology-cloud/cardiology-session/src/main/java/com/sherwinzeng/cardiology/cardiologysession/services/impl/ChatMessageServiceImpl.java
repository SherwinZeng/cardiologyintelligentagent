package com.sherwinzeng.cardiology.cardiologysession.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatMessage;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatMessageMapper;
import com.sherwinzeng.cardiology.cardiologysession.response.ChatMessageResponse;
import com.sherwinzeng.cardiology.cardiologysession.services.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;

    @Override
    public List<ChatMessageResponse> listBySession(String uid, String session) {
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getUid, uid)
                .eq(ChatMessage::getSessionId, session)
                .orderByAsc(ChatMessage::getCreatedAt);
        return chatMessageMapper.selectList(queryWrapper).stream()
                .map(this::toResponse)
                .toList();
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setRole(message.getRole());
        response.setContent(message.getContent());
        response.setUrgency(message.getUrgency());
        response.setExplanation(message.getExplanation());
        response.setAdvice(message.getAdvice());
        response.setDisclaimer(message.getDisclaimer());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
}
