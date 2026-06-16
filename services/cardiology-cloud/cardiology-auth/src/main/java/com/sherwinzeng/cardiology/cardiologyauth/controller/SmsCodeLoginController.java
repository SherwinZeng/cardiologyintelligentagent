package com.sherwinzeng.cardiology.cardiologyauth.controller;

import com.sherwinzeng.cardiology.cardiologyauth.request.SmsCodeSenderRequestParams;
import com.sherwinzeng.cardiology.cardiologyauth.request.SmsLoginRequestParams;
import com.sherwinzeng.cardiology.cardiologyauth.services.SmsCodeLoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/sms/login")
@RequiredArgsConstructor
public class SmsCodeLoginController {
    private final SmsCodeLoginService smsCodeLoginService;

    @PostMapping("/v1")
    public String smsLogin(@Valid @RequestBody SmsLoginRequestParams smsLoginRequestParams) {
        return smsCodeLoginService.smsLogin(smsLoginRequestParams);
    }

    @PostMapping("/captcha/v1")
    public String generateCaptcha(@Valid @RequestBody SmsCodeSenderRequestParams smsCodeSenderRequestParams) {
        return smsCodeLoginService.generateCaptcha(smsCodeSenderRequestParams);
    }

    @PostMapping("/sms/v1")
    public String smsSender(@Valid @RequestBody SmsCodeSenderRequestParams smsCodeSenderRequestParams) {
        return smsCodeLoginService.smsSender(smsCodeSenderRequestParams);
    }
}
