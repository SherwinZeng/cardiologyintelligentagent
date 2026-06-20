package com.sherwinzeng.cardiology.cardiologysession.response;

import lombok.Data;

import java.util.List;

@Data
public class ConsultationRecordPageResponse {

    private List<ConsultationRecordResponse> records;
    private long total;
    private long page;
    private long pageSize;
    private boolean hasMore;
}
