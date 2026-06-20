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

    List<ChatSession> selectSummaryCandidates(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("minMessages") int minMessages,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("limit") int limit
    );

    int markSummaryProcessing(
            @Param("sessionId") String sessionId,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("minMessages") int minMessages,
            @Param("attemptedAt") LocalDateTime attemptedAt
    );

    int markSummaryDone(
            @Param("sessionId") String sessionId,
            @Param("title") String title,
            @Param("generatedAt") LocalDateTime generatedAt
    );

    int markSummaryFailed(
            @Param("sessionId") String sessionId,
            @Param("error") String error,
            @Param("attemptedAt") LocalDateTime attemptedAt
    );

    int markSummaryPending(@Param("sessionId") String sessionId);

    int recoverStuckSummaryProcessing(@Param("stuckBefore") LocalDateTime stuckBefore);
}
