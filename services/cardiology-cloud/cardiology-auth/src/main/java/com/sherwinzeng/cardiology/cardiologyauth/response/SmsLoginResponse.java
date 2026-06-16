package com.sherwinzeng.cardiology.cardiologyauth.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmsLoginResponse {
    private String id;
    private String token;
    private String nickname;
    private String phone;
    private String avatar;
}
