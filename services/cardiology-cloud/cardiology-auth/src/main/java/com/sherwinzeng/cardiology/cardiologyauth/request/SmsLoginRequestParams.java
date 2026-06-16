package com.sherwinzeng.cardiology.cardiologyauth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SmsLoginRequestParams {
    @NotBlank(message = "手机号 不能为空")
    private String phone;

    @NotBlank(message = "短信验证码 不能为空")
    private String code;
}
