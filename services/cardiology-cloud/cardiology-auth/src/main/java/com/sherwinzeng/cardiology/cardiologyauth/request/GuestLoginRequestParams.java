package com.sherwinzeng.cardiology.cardiologyauth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GuestLoginRequestParams {
    @NotBlank(message = "id 不能为空")
    String id;
}
