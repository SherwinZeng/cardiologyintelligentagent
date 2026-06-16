package com.sherwinzeng.cardiology.cardiologysession.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionResponse {

    private String sessionId;
    private String uid;
    private String title;
    private String preview;
    private Integer messageCount;
    private String status;
    private Boolean pinned;
    private LocalDateTime pinnedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
