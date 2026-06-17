package com.sherwinzeng.cardiology.cardiologysession.controller;

import com.sherwinzeng.cardiology.cardiologycloudcommonutils.auth.AuthHeaders;
import com.sherwinzeng.cardiology.cardiologysession.request.GeneralUnderstandingRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.services.GeneralUnderstandingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/generalUnderstanding")
public class ChatGeneralUnderstandingController {
    private final GeneralUnderstandingService generalUnderstandingService;

    @PostMapping("/v1")
    public String generalUnderstanding(
            @Valid @RequestBody GeneralUnderstandingRequestParams generalUnderstandingRequestParams,
            @RequestHeader(value = AuthHeaders.USER_TYPE, required = false) String userType,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUid
    ) {
        return generalUnderstandingService.generalUnderstanding(
                generalUnderstandingRequestParams, userType, authenticatedUid);
    }
}
