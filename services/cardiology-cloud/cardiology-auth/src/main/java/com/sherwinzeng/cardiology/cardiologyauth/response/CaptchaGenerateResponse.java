package com.sherwinzeng.cardiology.cardiologyauth.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaptchaGenerateResponse {
    private String captchaId;
    private String captchaImage;
}
