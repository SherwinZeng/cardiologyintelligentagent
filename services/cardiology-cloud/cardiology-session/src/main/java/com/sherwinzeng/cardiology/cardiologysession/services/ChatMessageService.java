package com.sherwinzeng.cardiology.cardiologysession.services;

public interface ChatMessageService {

    String listMessages(
            String uid,
            String session,
            Long beforeId,
            Integer pageSize,
            String userType,
            String authenticatedUid
    );
}
