package com.sherwinzeng.cardiology.cardiologyauth.services.impl;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sherwinzeng.cardiology.cardiologyauth.components.JsonWebTokenFactory;
import com.sherwinzeng.cardiology.cardiologyauth.entity.User;
import com.sherwinzeng.cardiology.cardiologyauth.properties.AliyunProperties;
import com.sherwinzeng.cardiology.cardiologyauth.properties.AuthSmsProperties;
import com.sherwinzeng.cardiology.cardiologyauth.repository.UserRepository;
import com.sherwinzeng.cardiology.cardiologyauth.request.SmsCodeSenderRequestParams;
import com.sherwinzeng.cardiology.cardiologyauth.request.SmsLoginRequestParams;
import com.sherwinzeng.cardiology.cardiologyauth.response.SmsLoginResponse;
import com.sherwinzeng.cardiology.cardiologyauth.response.CaptchaGenerateResponse;
import com.sherwinzeng.cardiology.cardiologyauth.services.SmsCodeLoginService;
import com.sherwinzeng.cardiology.cardiologyauth.utils.SmsCaptchaValidator;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.auth.AuthUserType;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeLoginServiceImpl extends ServiceImpl<UserRepository, User> implements SmsCodeLoginService {

    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final AliyunProperties aliyunProperties;
    private final AuthSmsProperties authSmsProperties;
    private final JsonWebTokenFactory jsonWebTokenFactory;

    @Value("${jwt.token-validity:7200}")
    private Long jwtTokenValidity;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String smsLogin(SmsLoginRequestParams smsLoginRequestParams) {
        String phone = smsLoginRequestParams.getPhone();
        String smsCode = smsLoginRequestParams.getCode().trim();
        String codeKey = authSmsProperties.getCodeKey() + phone;
        try {
            String cachedSmsCode = stringRedisTemplate.opsForValue().get(codeKey);
            if (!StringUtils.hasText(cachedSmsCode) || !cachedSmsCode.equals(smsCode)) {
                log.warn("短信验证码校验失败，phone={}", phone);
                return JsonSerialization.toJson(BaseResponse.fail(ResponseCode.BAD_REQUEST, "短信验证码错误或已过期"));
            }
            User user = userRepository.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
            LocalDateTime now = LocalDateTime.now();
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setNickname("用户" + phone.substring(phone.length() - 4));
                user.setStatus(1);
                user.setLastLoginAt(now);
                userRepository.insert(user);
            } else {
                if (user.getStatus() != null && user.getStatus() != 1) {
                    log.warn("短信登录失败，账号已禁用，phone={}，userId={}", phone, user.getId());
                    return JsonSerialization.toJson(BaseResponse.fail(ResponseCode.FORBIDDEN, "账号已禁用"));
                }
                user.setLastLoginAt(now);
                userRepository.updateById(user);
            }
            stringRedisTemplate.delete(codeKey);

            String userId = String.valueOf(user.getId());
            String token = jsonWebTokenFactory.generateToken(userId, AuthUserType.FORMAL, jwtTokenValidity);
            String displayName = "用户" + phone.substring(phone.length() - 4);
            if (StringUtils.hasText(user.getNickname())) {
                displayName = user.getNickname().trim();
            }
            log.info("短信登录成功，phone={}，userId={}", phone, userId);
            return JsonSerialization.toJson(BaseResponse.success(new SmsLoginResponse(
                    userId,
                    token,
                    displayName,
                    phone,
                    user.getAvatar() != null ? user.getAvatar() : "")));
        } catch (Exception exception) {
            log.error("短信登录异常，phone={}", phone, exception);
            return JsonSerialization.toJson(BaseResponse.fail(ResponseCode.SERVER_ERROR, "登录失败，请稍后重试"));
        }
    }

    @Override
    public String generateCaptcha(SmsCodeSenderRequestParams smsCodeSenderRequestParams) {
        String phone = smsCodeSenderRequestParams.getPhone();
        try {
            int captchaLength = authSmsProperties.getCaptchaLength();
            String captchaChars = authSmsProperties.getCaptchaChars();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            StringBuilder captchaCodeBuilder = new StringBuilder(captchaLength);
            for (int index = 0; index < captchaLength; index++) {
                captchaCodeBuilder.append(captchaChars.charAt(random.nextInt(captchaChars.length())));
            }
            String captchaCode = captchaCodeBuilder.toString();
            String captchaId = UUID.randomUUID().toString().replace("-", "");
            int width = 120;
            int height = 40;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            for (int noiseIndex = 0; noiseIndex < 6; noiseIndex++) {
                graphics.setColor(new Color(random.nextInt(180), random.nextInt(180), random.nextInt(180)));
                graphics.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
            }
            for (int charIndex = 0; charIndex < captchaLength; charIndex++) {
                graphics.setColor(new Color(20 + random.nextInt(110), 20 + random.nextInt(110), 20 + random.nextInt(110)));
                graphics.drawString(String.valueOf(captchaCode.charAt(charIndex)), 18 + charIndex * 24, 30);
            }
            graphics.dispose();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            String captchaImageBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
            String redisKey = authSmsProperties.getCaptchaKey() + captchaId;
            stringRedisTemplate.opsForValue().set(
                    redisKey,
                    phone + ":" + captchaCode.toLowerCase(),
                    Duration.ofSeconds(authSmsProperties.getCaptchaValidSeconds()));
            log.info("图形验证码生成成功，phone={}，captchaId={}", phone, captchaId);
            return JsonSerialization.toJson(BaseResponse.success(new CaptchaGenerateResponse(captchaId, captchaImageBase64)));
        } catch (Exception exception) {
            log.error("图形验证码生成失败，phone={}", phone, exception);
            return JsonSerialization.toJson(BaseResponse.fail(ResponseCode.SERVER_ERROR, "图形验证码生成失败"));
        }
    }

    @Override
    public String smsSender(SmsCodeSenderRequestParams smsCodeSenderRequestParams) {
        String phone = smsCodeSenderRequestParams.getPhone();
        String lockKey = authSmsProperties.getSendLockKey() + phone;
        String codeKey = authSmsProperties.getCodeKey() + phone;
        try {
            if (!SmsCaptchaValidator.validate(
                    stringRedisTemplate,
                    authSmsProperties.getCaptchaKey(),
                    phone,
                    smsCodeSenderRequestParams.getCaptchaId(),
                    smsCodeSenderRequestParams.getCaptchaCode())) {
                log.warn("图形验证码校验失败，phone={}，captchaId={}", phone, smsCodeSenderRequestParams.getCaptchaId());
                return JsonSerialization.toJson(BaseResponse.fail(ResponseCode.BAD_REQUEST, "图形验证码错误或已过期"));
            }
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
                log.warn("短信发送过于频繁（本地频控），phone={}", phone);
                return JsonSerialization.toJson(BaseResponse.fail(ResponseCode.BAD_REQUEST, "发送太频繁，请稍后再试"));
            }
            String verifyCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            Config config = new Config()
                    .setAccessKeyId(aliyunProperties.getAccessKeyId())
                    .setAccessKeySecret(aliyunProperties.getAccessKeySecret());
            config.endpoint = "dypnsapi.aliyuncs.com";
            Client client = new Client(config);
            long validSeconds = authSmsProperties.getCodeValidMinutes() * 60;
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setSignName(authSmsProperties.getSignName())
                    .setTemplateCode(authSmsProperties.getTemplateCode())
                    .setTemplateParam("{\"code\":\"" + verifyCode + "\",\"min\":\"" + authSmsProperties.getCodeValidMinutes() + "\"}")
                    .setValidTime(validSeconds);
            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCodeWithOptions(request, new RuntimeOptions());
            if (response == null || response.getBody() == null || !"OK".equals(response.getBody().getCode())) {
                String errorMessage = response != null && response.getBody() != null
                        ? response.getBody().getMessage()
                        : "未知错误";
                if (errorMessage.toLowerCase().contains("frequency")) {
                    stringRedisTemplate.opsForValue().set(lockKey, "1", Duration.ofSeconds(authSmsProperties.getSendInterval()));
                    log.warn("短信发送过于频繁（阿里云频控），phone={}", phone);
                    return JsonSerialization.toJson(BaseResponse.fail(ResponseCode.BAD_REQUEST, "发送太频繁，请稍后再试"));
                }
                log.warn("短信发送失败，phone={}，原因={}", phone, errorMessage);
                return JsonSerialization.toJson(BaseResponse.fail(ResponseCode.SERVER_ERROR, "短信发送失败，请稍后重试"));
            }
            stringRedisTemplate.opsForValue().set(codeKey, verifyCode, Duration.ofSeconds(validSeconds));
            stringRedisTemplate.opsForValue().set(lockKey, "1", Duration.ofSeconds(authSmsProperties.getSendInterval()));
            log.info("短信验证码发送成功，phone={}，codeKey={}，ttl={}s", phone, codeKey, validSeconds);
            return JsonSerialization.toJson(BaseResponse.success(null));
        } catch (Exception exception) {
            log.error("短信发送异常，phone={}", phone, exception);
            return JsonSerialization.toJson(BaseResponse.fail(ResponseCode.SERVER_ERROR, "短信发送失败，请稍后重试"));
        }
    }
}
