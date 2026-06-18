package com.sherwinzeng.cardiology.cardiologysession.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.exception.ChatBusinessException;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatMessage;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.entity.chat.ChatSessionStatus;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatMessageMapper;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatSessionMapper;
import com.sherwinzeng.cardiology.cardiologysession.feign.DRFAgentFeignClient;
import com.sherwinzeng.cardiology.cardiologysession.request.CheckpointDeleteRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.request.CreateChatSessionRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.request.PinChatSessionRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.response.ChatSessionPageResponse;
import com.sherwinzeng.cardiology.cardiologysession.response.ChatSessionResponse;
import com.sherwinzeng.cardiology.cardiologysession.services.ChatSessionService;
import com.sherwinzeng.cardiology.cardiologysession.store.GuestChatSessionStore;
import com.sherwinzeng.cardiology.cardiologysession.support.AuthHeaderSupport;
import com.sherwinzeng.cardiology.cardiologysession.support.FormalChatSessionSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 50;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final GuestChatSessionStore guestChatSessionStore;
    private final DRFAgentFeignClient drfAgentFeignClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSession(CreateChatSessionRequestParams createChatSessionRequestParams,
                                String userType,
                                String authenticatedUid) {
        AuthHeaderSupport.assertUidMatch(createChatSessionRequestParams.getUid(), authenticatedUid);
        if (AuthHeaderSupport.isGuest(userType)) {
            ChatSession chatSession = guestChatSessionStore.createSession(
                    createChatSessionRequestParams.getUid(),
                    createChatSessionRequestParams.getSession()
            );
            return JsonSerialization.toJson(BaseResponse.success(chatSession));
        }

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
    public String listSessions(String uid, Integer page, Integer pageSize, String keyword,
                               String userType,
                               String authenticatedUid) {
        AuthHeaderSupport.assertUidMatch(uid, authenticatedUid);
        int resolvedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int resolvedPageSize = pageSize == null || pageSize < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);

        ChatSessionPageResponse pageResponse = new ChatSessionPageResponse();
        if (AuthHeaderSupport.isGuest(userType)) {
            List<ChatSession> allSessions = guestChatSessionStore.listSessions(uid, keyword);
            int total = allSessions.size();
            int fromIndex = Math.min((resolvedPage - 1) * resolvedPageSize, total);
            int toIndex = Math.min(fromIndex + resolvedPageSize, total);
            pageResponse.setRecords(allSessions.subList(fromIndex, toIndex).stream().map(session -> {
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
            pageResponse.setTotal(total);
            pageResponse.setPage(resolvedPage);
            pageResponse.setPageSize(resolvedPageSize);
            pageResponse.setHasMore((long) resolvedPage * resolvedPageSize < total);
            return JsonSerialization.toJson(BaseResponse.success(pageResponse));
        }

        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<ChatSession> pageQuery = new Page<>(resolvedPage, resolvedPageSize);
        IPage<ChatSession> result = chatSessionMapper.selectActivePage(pageQuery, uid, trimmedKeyword);
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
    public String deleteSession(String uid, String sessionId, String userType, String authenticatedUid) {
        AuthHeaderSupport.assertUidMatch(uid, authenticatedUid);
        if (AuthHeaderSupport.isGuest(userType)) {
            guestChatSessionStore.deleteSession(uid, sessionId);
        } else {

            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session == null) {
                throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "会话不存在");
            }
            if (!uid.equals(session.getUid())) {
                throw new ChatBusinessException(ResponseCode.FORBIDDEN, "无权删除该会话");
            }
            LambdaQueryWrapper<ChatMessage> messageQuery = new LambdaQueryWrapper<>();
            messageQuery.eq(ChatMessage::getUid, uid).eq(ChatMessage::getSessionId, sessionId);
            chatMessageMapper.delete(messageQuery);
            chatSessionMapper.deleteById(sessionId);
        }
        deleteAiAgentCheckpoint(uid, sessionId);
        return JsonSerialization.toJson(BaseResponse.success(null));
    }

    private void deleteAiAgentCheckpoint(String uid, String sessionId) {
        try {
            String token = UUID.randomUUID().toString();
            stringRedisTemplate.opsForValue().set("internal:token:" + token, "ok", 60, TimeUnit.SECONDS);
            CheckpointDeleteRequestParams params = new CheckpointDeleteRequestParams();
            params.setUid(uid);
            params.setSession(sessionId);
            drfAgentFeignClient.deleteCheckpoint(token, params);
        } catch (Exception exception) {
            log.warn("删除 LangGraph checkpoint 失败 | uid={} session={} err={}", uid, sessionId, exception.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String pinSession(PinChatSessionRequestParams params, String userType, String authenticatedUid) {
        AuthHeaderSupport.assertUidMatch(params.getUid(), authenticatedUid);
        if (AuthHeaderSupport.isGuest(userType)) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "游客不支持置顶");
        }

        ChatSession session = FormalChatSessionSupport.requireOwnedSession(chatSessionMapper, params.getUid(), params.getSession());
        if (ChatSessionStatus.ARCHIVED.equals(session.getStatus())) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "会话已归档，无法置顶");
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
