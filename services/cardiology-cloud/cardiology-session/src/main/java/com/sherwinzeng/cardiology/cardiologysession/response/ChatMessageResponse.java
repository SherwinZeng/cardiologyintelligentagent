package com.sherwinzeng.cardiology.cardiologysession.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatMessageResponse {

    private Long id;
    private String role;
    private String content;
    private String urgency;
    private String explanation;
    private String advice;
    private String disclaimer;
    private List<String> guideReferences;
    private LocalDateTime createdAt;
}
