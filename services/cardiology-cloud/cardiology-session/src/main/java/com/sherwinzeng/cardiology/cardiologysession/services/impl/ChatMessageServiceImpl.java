package com.sherwinzeng.cardiology.cardiologysession.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatMessage;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatMessageMapper;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatSessionMapper;
import com.sherwinzeng.cardiology.cardiologysession.response.ChatMessagePageResponse;
import com.sherwinzeng.cardiology.cardiologysession.response.ChatMessageResponse;
import com.sherwinzeng.cardiology.cardiologysession.services.ChatMessageService;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import com.sherwinzeng.cardiology.cardiologysession.store.GuestChatMessagePayload;
import com.sherwinzeng.cardiology.cardiologysession.store.GuestChatSessionStore;
import com.sherwinzeng.cardiology.cardiologysession.support.AuthHeaderSupport;
import com.sherwinzeng.cardiology.cardiologysession.support.FormalChatSessionSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final int DEFAULT_PAGE_SIZE = 40;
    private static final int MAX_PAGE_SIZE = 100;

    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final GuestChatSessionStore guestChatSessionStore;

    @Override
    public String listMessages(String uid, String session, Long beforeId, Integer pageSize,
                               String userType, String authenticatedUid) {
        AuthHeaderSupport.assertUidMatch(uid, authenticatedUid);
        int resolvedPageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        int fetchSize = resolvedPageSize + 1;

        if (AuthHeaderSupport.isGuest(userType)) {
            List<GuestChatMessagePayload> batch = guestChatSessionStore.listMessages(uid, session, beforeId, fetchSize);
            boolean hasMore = batch.size() > resolvedPageSize;
            if (hasMore) {
                batch = new ArrayList<>(batch.subList(0, resolvedPageSize));
            }
            List<ChatMessageResponse> records = new ArrayList<>(batch.size());
            for (GuestChatMessagePayload message : batch) {
                ChatMessageResponse response = new ChatMessageResponse();
                response.setId(message.getId());
                response.setRole(message.getRole());
                response.setContent(message.getContent());
                response.setUrgency(message.getUrgency());
                response.setExplanation(message.getExplanation());
                response.setAdvice(message.getAdvice());
                response.setDisclaimer(message.getDisclaimer());
                response.setCreatedAt(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(message.getCreatedAt()), ZoneId.systemDefault()));
                records.add(response);
            }
            ChatMessagePageResponse pageResponse = new ChatMessagePageResponse();
            pageResponse.setRecords(records);
            pageResponse.setTotal(guestChatSessionStore.countMessages(uid, session));
            pageResponse.setPageSize(resolvedPageSize);
            pageResponse.setHasMore(hasMore);
            return JsonSerialization.toJson(BaseResponse.success(pageResponse));
        }

        FormalChatSessionSupport.requireOwnedSession(chatSessionMapper, uid, session);

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getUid, uid).eq(ChatMessage::getSessionId, session);
        if (beforeId != null && beforeId > 0) {
            queryWrapper.lt(ChatMessage::getId, beforeId);
        }
        queryWrapper.orderByDesc(ChatMessage::getId).last("LIMIT " + fetchSize);
        List<ChatMessage> batch = chatMessageMapper.selectList(queryWrapper);
        boolean hasMore = batch.size() > resolvedPageSize;
        if (hasMore) {
            batch = new ArrayList<>(batch.subList(0, resolvedPageSize));
        }
        List<ChatMessage> ordered = new ArrayList<>(batch);
        Collections.reverse(ordered);
        List<ChatMessageResponse> records = new ArrayList<>(ordered.size());
        for (ChatMessage message : ordered) {
            ChatMessageResponse response = new ChatMessageResponse();
            response.setId(message.getId());
            response.setRole(message.getRole());
            response.setContent(message.getContent());
            response.setUrgency(message.getUrgency());
            response.setExplanation(message.getExplanation());
            response.setAdvice(message.getAdvice());
            response.setDisclaimer(message.getDisclaimer());
            response.setCreatedAt(message.getCreatedAt());
            records.add(response);
        }
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
}
