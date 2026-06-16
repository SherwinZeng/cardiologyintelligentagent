package com.sherwinzeng.cardiology.cardiologyauth.services.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sherwinzeng.cardiology.cardiologyauth.components.JsonWebTokenFactory;
import com.sherwinzeng.cardiology.cardiologyauth.entity.User;
import com.sherwinzeng.cardiology.cardiologyauth.properties.AuthGuestProperties;
import com.sherwinzeng.cardiology.cardiologyauth.repository.UserRepository;
import com.sherwinzeng.cardiology.cardiologyauth.request.GuestLoginRequestParams;
import com.sherwinzeng.cardiology.cardiologyauth.response.GuestLoginResponse;
import com.sherwinzeng.cardiology.cardiologyauth.services.GuestLoginService;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestLoginServiceImpl extends ServiceImpl<UserRepository, User> implements GuestLoginService {
    private final RedisTemplate<String, String> redisTemplate;
    private final AuthGuestProperties authGuestProperties;
    private final JsonWebTokenFactory jsonWebTokenFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String guestLogin(GuestLoginRequestParams guestLoginRequestParams) {
        String redisKey = authGuestProperties.getKey() + guestLoginRequestParams.getId();
        String cachedToken = redisTemplate.opsForValue().get(redisKey);
        if (cachedToken != null && !cachedToken.isBlank()) {
            redisTemplate.delete(redisKey);
            return JsonSerialization.toJson(BaseResponse.fail(ResponseCode.FORBIDDEN, "账号已在其他设备登录，已强制下线"));
        }
        String token = jsonWebTokenFactory.generateToken(guestLoginRequestParams.getId(), authGuestProperties.getTime());
        redisTemplate.opsForValue().set(redisKey, token, Duration.ofSeconds(authGuestProperties.getTime()));
        return JsonSerialization.toJson(BaseResponse.success(new GuestLoginResponse(guestLoginRequestParams.getId(), token)));
    }
}
