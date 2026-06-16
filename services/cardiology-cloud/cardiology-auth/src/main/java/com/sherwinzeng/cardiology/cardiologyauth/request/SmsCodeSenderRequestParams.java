package com.sherwinzeng.cardiology.cardiologyauth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SmsCodeSenderRequestParams {
    @NotBlank(message = "手机号 不能为空")
    private String phone;

    private String captchaId;

    private String captchaCode;
}
