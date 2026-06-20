package com.sherwinzeng.cardiology.cardiologysession.services;

public interface ConsultationRecordService {

    String listRecords(String uid, Integer page, Integer pageSize, String urgency, String keyword,
            String startDate, String endDate, String userType, String authenticatedUid);
}
