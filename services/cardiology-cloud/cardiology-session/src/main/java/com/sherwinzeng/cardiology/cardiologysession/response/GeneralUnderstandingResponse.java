package com.sherwinzeng.cardiology.cardiologysession.response;

import lombok.Data;

import java.util.List;

@Data
public class GeneralUnderstandingResponse {
    private String urgency;
    private String explanation;
    private String advice;
    private String disclaimer;
    private List<String> guideReferences;
}
