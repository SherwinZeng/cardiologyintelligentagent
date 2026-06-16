package com.sherwinzeng.cardiology.cardiologysession.response;

import lombok.Data;

import java.util.List;

@Data
public class ChatSessionPageResponse {

    private List<ChatSessionResponse> records;
    private long total;
    private long page;
    private long pageSize;
    private boolean hasMore;
}
