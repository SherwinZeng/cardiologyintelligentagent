package com.sherwinzeng.cardiology.cardiologysession.store;

import lombok.Data;

import java.util.List;

@Data
public class GuestChatMessagePayload {

    private long id;
    private String role;
    private String content;
    private String urgency;
    private String explanation;
    private String advice;
    private String disclaimer;
    private List<String> guideReferences;
    private long createdAt;
}
