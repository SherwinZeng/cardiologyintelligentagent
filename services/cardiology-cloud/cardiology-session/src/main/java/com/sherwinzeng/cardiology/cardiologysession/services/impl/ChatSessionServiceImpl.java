package com.sherwinzeng.cardiology.cardiologysession.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.exception.ChatBusinessException;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatMessage;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatSessionStatus;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatMessageMapper;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatSessionMapper;
import com.sherwinzeng.cardiology.cardiologysession.request.CreateChatSessionRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.request.PinChatSessionRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.response.ChatSessionPageResponse;
import com.sherwinzeng.cardiology.cardiologysession.response.ChatSessionResponse;
import com.sherwinzeng.cardiology.cardiologysession.services.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 50;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

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
        chatSession.setPinned(false);
        chatSessionMapper.insert(chatSession);
        return JsonSerialization.toJson(BaseResponse.success(chatSession));
    }

    @Override
    public String listSessions(String uid, Integer page, Integer pageSize, String keyword) {
        int resolvedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int resolvedPageSize = pageSize == null || pageSize < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<ChatSession> pageQuery = new Page<>(resolvedPage, resolvedPageSize);
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getUid, uid);
        if (trimmedKeyword != null) {
            queryWrapper.and(wrapper -> wrapper
                    .like(ChatSession::getTitle, trimmedKeyword)
                    .or()
                    .like(ChatSession::getPreview, trimmedKeyword));
        }
        queryWrapper.orderByDesc(ChatSession::getPinned)
                .orderByDesc(ChatSession::getPinnedAt)
                .orderByDesc(ChatSession::getUpdatedAt);
        Page<ChatSession> result = chatSessionMapper.selectPage(pageQuery, queryWrapper);

        ChatSessionPageResponse pageResponse = new ChatSessionPageResponse();
        pageResponse.setRecords(result.getRecords().stream().map(session -> {
            ChatSessionResponse response = new ChatSessionResponse();
            response.setSessionId(session.getSessionId());
            response.setUid(session.getUid());
            response.setTitle(session.getTitle());
            response.setPreview(session.getPreview());
            response.setMessageCount(session.getMessageCount());
            response.setStatus(session.getStatus());
            response.setPinned(Boolean.TRUE.equals(session.getPinned()));
            response.setPinnedAt(session.getPinnedAt());
            response.setCreatedAt(session.getCreatedAt());
            response.setUpdatedAt(session.getUpdatedAt());
            return response;
        }).toList());
        pageResponse.setTotal(result.getTotal());
        pageResponse.setPage(result.getCurrent());
        pageResponse.setPageSize(result.getSize());
        pageResponse.setHasMore(result.getCurrent() * result.getSize() < result.getTotal());
        return JsonSerialization.toJson(BaseResponse.success(pageResponse));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteSession(String uid, String sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "会话不存在");
        }
        if (!uid.equals(session.getUid())) {
            throw new ChatBusinessException(ResponseCode.FORBIDDEN, "无权删除该会话");
        }

        LambdaQueryWrapper<ChatMessage> messageQuery = new LambdaQueryWrapper<>();
        messageQuery.eq(ChatMessage::getUid, uid)
                .eq(ChatMessage::getSessionId, sessionId);
        chatMessageMapper.delete(messageQuery);
        chatSessionMapper.deleteById(sessionId);
        return JsonSerialization.toJson(BaseResponse.success(null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String pinSession(PinChatSessionRequestParams params) {
        ChatSession session = chatSessionMapper.selectById(params.getSession());
        if (session == null) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "会话不存在");
        }
        if (!params.getUid().equals(session.getUid())) {
            throw new ChatBusinessException(ResponseCode.FORBIDDEN, "无权操作该会话");
        }

        ChatSession update = new ChatSession();
        update.setSessionId(params.getSession());
        if (Boolean.TRUE.equals(params.getPinned())) {
            update.setPinned(true);
            update.setPinnedAt(LocalDateTime.now());
        } else {
            update.setPinned(false);
            update.setPinnedAt(null);
        }
        chatSessionMapper.updateById(update);
        return JsonSerialization.toJson(BaseResponse.success(null));
    }
}
