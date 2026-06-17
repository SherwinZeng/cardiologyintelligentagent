package com.sherwinzeng.cardiology.cardiologysession.store;

import lombok.Data;

@Data
public class GuestChatMessagePayload {

    private long id;
    private String role;
    private String content;
    private String urgency;
    private String explanation;
    private String advice;
    private String disclaimer;
    private long createdAt;
}
