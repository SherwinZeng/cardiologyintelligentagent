package com.sherwinzeng.cardiology.cardiologysession.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PinChatSessionRequestParams {

    @NotBlank(message = "uid 不能为空")
    private String uid;

    @NotBlank(message = "session 不能为空")
    private String session;

    @NotNull(message = "pinned 不能为空")
    private Boolean pinned;
}
