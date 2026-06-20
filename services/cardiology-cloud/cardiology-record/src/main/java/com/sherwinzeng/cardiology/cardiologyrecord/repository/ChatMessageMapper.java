package com.sherwinzeng.cardiology.cardiologyrecord.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sherwinzeng.cardiology.cardiologyrecord.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    int deleteBySessionId(@Param("sessionId") String sessionId);

    List<ChatMessage> selectSessionMessages(
            @Param("uid") String uid,
            @Param("sessionId") String sessionId,
            @Param("limit") int limit
    );
}
