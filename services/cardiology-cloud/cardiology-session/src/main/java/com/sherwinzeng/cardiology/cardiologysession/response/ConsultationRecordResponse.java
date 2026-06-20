package com.sherwinzeng.cardiology.cardiologysession.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationRecordResponse {

    private Long id;
    private String sessionId;
    private String uid;
    private String title;
    private String urgency;
    private String summary;
    private Integer messageCount;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime generatedAt;
}
