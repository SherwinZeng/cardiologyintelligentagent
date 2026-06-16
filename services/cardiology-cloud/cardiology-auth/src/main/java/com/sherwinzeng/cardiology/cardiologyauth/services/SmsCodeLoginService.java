package com.sherwinzeng.cardiology.cardiologyauth.services;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sherwinzeng.cardiology.cardiologyauth.entity.User;
import com.sherwinzeng.cardiology.cardiologyauth.request.SmsCodeSenderRequestParams;
import com.sherwinzeng.cardiology.cardiologyauth.request.SmsLoginRequestParams;

public interface SmsCodeLoginService extends IService<User> {
    String generateCaptcha(SmsCodeSenderRequestParams smsCodeSenderRequestParams);

    String smsSender(SmsCodeSenderRequestParams smsCodeSenderRequestParams);

    String smsLogin(SmsLoginRequestParams smsLoginRequestParams);
}
