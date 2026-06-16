package com.sherwinzeng.cardiology.cardiologysession.services.impl;

import com.sherwinzeng.cardiology.cardiologycloudcommondata.exception.ChatBusinessException;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatMessage;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatMessageRole;
import com.sherwinzeng.cardiology.cardiologysession.repository.ChatMessageMapper;
import com.sherwinzeng.cardiology.cardiologysession.request.GeneralUnderstandingRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.feign.DRFAgentFeignClient;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologysession.response.GeneralUnderstandingResponse;
import com.sherwinzeng.cardiology.cardiologysession.services.GeneralUnderstandingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneralUnderstandingServiceImpl implements GeneralUnderstandingService {
    private final DRFAgentFeignClient drfAgentFeignClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generalUnderstanding(GeneralUnderstandingRequestParams generalUnderstandingRequestParams) throws ChatBusinessException {
        try {
            String token = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set("internal:token:" + token, "ok", 60, TimeUnit.SECONDS);
            ChatMessage humanMessage = new ChatMessage();
            humanMessage.setSessionId(generalUnderstandingRequestParams.getSession());
            humanMessage.setUid(generalUnderstandingRequestParams.getUid());
            humanMessage.setRole(ChatMessageRole.USER);
            humanMessage.setContent(generalUnderstandingRequestParams.getMessage());
            chatMessageMapper.insert(humanMessage);
            BaseResponse<GeneralUnderstandingResponse> generalUnderstandingResponseBaseResponse =
                    drfAgentFeignClient.generalUnderstanding(token, generalUnderstandingRequestParams);
            ChatMessage assistantMsg = new ChatMessage();
            assistantMsg.setSessionId(generalUnderstandingRequestParams.getSession());
            assistantMsg.setUid(generalUnderstandingRequestParams.getUid());
            assistantMsg.setRole(ChatMessageRole.ASSISTANT);
            assistantMsg.setContent(generalUnderstandingResponseBaseResponse.getData().getExplanation());
            assistantMsg.setUrgency(generalUnderstandingResponseBaseResponse.getData().getUrgency());
            assistantMsg.setExplanation(generalUnderstandingResponseBaseResponse.getData().getExplanation());
            assistantMsg.setAdvice(generalUnderstandingResponseBaseResponse.getData().getAdvice());
            assistantMsg.setDisclaimer(generalUnderstandingResponseBaseResponse.getData().getDisclaimer());
            chatMessageMapper.insert(assistantMsg);
            log.info(
                    "铭铭回答了 | uid={} session={} urgency={} explanation={}",
                    generalUnderstandingRequestParams.getUid(),
                    generalUnderstandingRequestParams.getSession(),
                    generalUnderstandingResponseBaseResponse.getData().getUrgency(),
                    generalUnderstandingResponseBaseResponse.getData().getExplanation().length() >= 80 ?
                            generalUnderstandingResponseBaseResponse.getData().getExplanation().substring(0, 80) + "....." :
                            generalUnderstandingResponseBaseResponse.getData().getExplanation()
            );
            return JsonSerialization.toJson(generalUnderstandingResponseBaseResponse);
        } catch (ChatBusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ChatBusinessException(exception.getMessage(), exception);
        }
    }
}
