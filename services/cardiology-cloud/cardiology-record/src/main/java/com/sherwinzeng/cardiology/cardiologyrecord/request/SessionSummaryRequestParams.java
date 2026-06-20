package com.sherwinzeng.cardiology.cardiologyrecord.request;

import lombok.Data;

import java.util.List;

@Data
public class SessionSummaryRequestParams {

    private String uid;
    private String session;
    private Integer messageCount;
    private List<MessageTurn> messages;

    @Data
    public static class MessageTurn {
        private String role;
        private String content;
        private String urgency;
        private String advice;
        private String disclaimer;
        private String createdAt;
    }
}
