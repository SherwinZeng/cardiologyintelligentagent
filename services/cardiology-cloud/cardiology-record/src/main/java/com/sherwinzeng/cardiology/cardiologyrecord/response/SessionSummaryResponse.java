package com.sherwinzeng.cardiology.cardiologyrecord.response;

import lombok.Data;

@Data
public class SessionSummaryResponse {

    private String title;
    private String urgency;
    private String summary;
}
