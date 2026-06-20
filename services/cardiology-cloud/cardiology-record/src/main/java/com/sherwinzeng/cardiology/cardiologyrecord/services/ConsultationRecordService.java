package com.sherwinzeng.cardiology.cardiologyrecord.services;

import com.sherwinzeng.cardiology.cardiologyrecord.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologyrecord.response.SessionSummaryResponse;

public interface ConsultationRecordService {

    void persistSummary(ChatSession session, SessionSummaryResponse summary);
}
