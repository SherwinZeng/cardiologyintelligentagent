package com.sherwinzeng.cardiology.cardiologysession.feign;

import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologysession.request.CheckpointDeleteRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.request.GeneralUnderstandingRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.response.GeneralUnderstandingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


@FeignClient(name = "drf-agent", url = "${cardiology.ai-agent.base-url}")
public interface DRFAgentFeignClient {

    @PostMapping("general-understanding/")
    BaseResponse<GeneralUnderstandingResponse> generalUnderstanding(
            @RequestHeader("X-Internal-Token") String internalToken,
            @RequestBody GeneralUnderstandingRequestParams generalUnderstandingRequestParams
    );

    @DeleteMapping("general-understanding/")
    BaseResponse<Void> deleteCheckpoint(
            @RequestHeader("X-Internal-Token") String internalToken,
            @RequestBody CheckpointDeleteRequestParams params
    );
}
