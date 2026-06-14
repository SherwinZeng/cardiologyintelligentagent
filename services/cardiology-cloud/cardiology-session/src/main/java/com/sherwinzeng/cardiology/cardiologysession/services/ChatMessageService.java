package com.sherwinzeng.cardiology.cardiologysession.services;

import com.sherwinzeng.cardiology.cardiologysession.response.ChatMessageResponse;

import java.util.List;

public interface ChatMessageService {

    List<ChatMessageResponse> listBySession(String uid, String session);
}
