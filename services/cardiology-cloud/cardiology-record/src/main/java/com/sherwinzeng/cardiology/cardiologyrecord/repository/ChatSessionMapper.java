package com.sherwinzeng.cardiology.cardiologyrecord.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sherwinzeng.cardiology.cardiologyrecord.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    List<ChatSession> selectInactiveActiveSessions(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("limit") int limit
    );

    List<ChatSession> selectExpiredArchivedSessions(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("limit") int limit
    );
}
