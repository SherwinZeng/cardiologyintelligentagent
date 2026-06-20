package com.sherwinzeng.cardiology.cardiologysession.feign;

import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologysession.request.CheckpointDeleteRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.request.GeneralUnderstandingRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.response.GeneralUnderstandingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DRFAgentFeignFallbackFactory implements FallbackFactory<DRFAgentFeignClient> {

    private static final String BUSY_MESSAGE = "铭铭暂时繁忙，请稍后再试";

    @Override
    public DRFAgentFeignClient create(Throwable cause) {
        return new DRFAgentFeignClient() {
            @Override
            public BaseResponse<GeneralUnderstandingResponse> generalUnderstanding(
                    String internalToken,
                    GeneralUnderstandingRequestParams generalUnderstandingRequestParams
            ) {
                log.warn(
                        "ai-agent generalUnderstanding 降级 | session={} cause={}",
                        generalUnderstandingRequestParams != null ? generalUnderstandingRequestParams.getSession() : null,
                        cause == null ? null : cause.toString()
                );
                return BaseResponse.fail(ResponseCode.SERVICE_UNAVAILABLE, BUSY_MESSAGE);
            }

            @Override
            public BaseResponse<Void> deleteCheckpoint(String internalToken, CheckpointDeleteRequestParams params) {
                log.warn(
                        "ai-agent deleteCheckpoint 降级 | session={} cause={}",
                        params != null ? params.getSession() : null,
                        cause == null ? null : cause.toString()
                );
                return BaseResponse.fail(ResponseCode.SERVICE_UNAVAILABLE, BUSY_MESSAGE);
            }
        };
    }
}
