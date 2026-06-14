package com.sherwinzeng.cardiology.cardiologysession.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GeneralUnderstandingRequestParams {

    @NotBlank(message = "uid 不能为空")
    private String uid;

    @NotBlank(message = "session 不能为空")
    private String session;

    @NotBlank(message = "message 不能为空")
    private String message;
}
