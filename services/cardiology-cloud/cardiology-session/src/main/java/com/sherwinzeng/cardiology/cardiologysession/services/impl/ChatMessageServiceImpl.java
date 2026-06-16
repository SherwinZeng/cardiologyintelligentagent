package com.sherwinzeng.cardiology.cardiologysession.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatMessage;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatMessageMapper;
import com.sherwinzeng.cardiology.cardiologysession.response.ChatMessagePageResponse;
import com.sherwinzeng.cardiology.cardiologysession.response.ChatMessageResponse;
import com.sherwinzeng.cardiology.cardiologysession.services.ChatMessageService;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final int DEFAULT_PAGE_SIZE = 40;
    private static final int MAX_PAGE_SIZE = 100;

    private final ChatMessageMapper chatMessageMapper;

    @Override
    public String listMessages(
            String uid,
            String session,
            Long beforeId,
            Integer pageSize
    ) {
        int resolvedPageSize = pageSize == null || pageSize < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
        int fetchSize = resolvedPageSize + 1;

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getUid, uid)
                .eq(ChatMessage::getSessionId, session);
        if (beforeId != null && beforeId > 0) {
            queryWrapper.lt(ChatMessage::getId, beforeId);
        }
        queryWrapper.orderByDesc(ChatMessage::getId)
                .last("LIMIT " + fetchSize);

        List<ChatMessage> batch = chatMessageMapper.selectList(queryWrapper);
        boolean hasMore = batch.size() > resolvedPageSize;
        if (hasMore) {
            batch = new ArrayList<>(batch.subList(0, resolvedPageSize));
        }

        List<ChatMessage> ordered = new ArrayList<>(batch);
        Collections.reverse(ordered);

        List<ChatMessageResponse> records = ordered.stream()
                .map(this::toResponse)
                .toList();

        long total = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getUid, uid)
                .eq(ChatMessage::getSessionId, session));

        ChatMessagePageResponse pageResponse = new ChatMessagePageResponse();
        pageResponse.setRecords(records);
        pageResponse.setTotal(total);
        pageResponse.setPageSize(resolvedPageSize);
        pageResponse.setHasMore(hasMore);
        return JsonSerialization.toJson(BaseResponse.success(pageResponse));
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
