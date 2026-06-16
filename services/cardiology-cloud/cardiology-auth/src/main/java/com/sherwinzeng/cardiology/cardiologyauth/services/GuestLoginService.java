package com.sherwinzeng.cardiology.cardiologyauth.services;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sherwinzeng.cardiology.cardiologyauth.entity.User;
import com.sherwinzeng.cardiology.cardiologyauth.request.GuestLoginRequestParams;

public interface GuestLoginService extends IService<User> {
    String guestLogin(GuestLoginRequestParams guestLoginRequestParams);
}
