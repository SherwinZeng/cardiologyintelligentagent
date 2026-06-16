package com.sherwinzeng.cardiology.cardiologysession.response;

import lombok.Data;

import java.util.List;

@Data
public class ChatMessagePageResponse {

    private List<ChatMessageResponse> records;
    private long total;
    private int pageSize;
    private boolean hasMore;
}
