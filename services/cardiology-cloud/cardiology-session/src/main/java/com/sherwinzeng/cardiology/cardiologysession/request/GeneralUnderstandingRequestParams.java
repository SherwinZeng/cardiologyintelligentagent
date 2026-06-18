package com.sherwinzeng.cardiology.cardiologysession.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class GeneralUnderstandingRequestParams {

    @NotBlank(message = "uid 不能为空")
    private String uid;

    @NotBlank(message = "session 不能为空")
    private String session;

    @NotBlank(message = "message 不能为空")
    private String message;

    private List<HistoryTurn> history = Collections.emptyList();

    @Data
    public static class HistoryTurn {
        private String role;
        private String content;
    }
}
