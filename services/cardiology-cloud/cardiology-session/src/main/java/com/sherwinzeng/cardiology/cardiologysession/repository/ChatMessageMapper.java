package com.sherwinzeng.cardiology.cardiologysession.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 某 session 最近 N 条消息（id 倒序）；Service 层 reverse 后作为 ai-agent history。
     */
    List<ChatMessage> selectRecentBySession(
            @Param("uid") String uid,
            @Param("sessionId") String sessionId,
            @Param("limit") int limit
    );
}
