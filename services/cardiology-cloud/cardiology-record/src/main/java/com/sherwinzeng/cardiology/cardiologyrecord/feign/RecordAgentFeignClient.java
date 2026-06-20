package com.sherwinzeng.cardiology.cardiologyrecord.feign;

import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologyrecord.request.SessionSummaryRequestParams;
import com.sherwinzeng.cardiology.cardiologyrecord.response.SessionSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "record-ai-agent", url = "${cardiology.ai-agent.base-url}")
public interface RecordAgentFeignClient {

    @PostMapping("session-summary/")
    BaseResponse<SessionSummaryResponse> summarizeSession(
            @RequestHeader("X-Internal-Token") String internalToken,
            @RequestBody SessionSummaryRequestParams params
    );
}
