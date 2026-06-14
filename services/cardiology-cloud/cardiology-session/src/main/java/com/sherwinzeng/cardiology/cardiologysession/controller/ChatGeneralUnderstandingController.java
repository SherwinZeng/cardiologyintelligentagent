package com.sherwinzeng.cardiology.cardiologysession.controller;

import com.sherwinzeng.cardiology.cardiologysession.request.GeneralUnderstandingRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.services.GeneralUnderstandingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/generalUnderstanding")
public class ChatGeneralUnderstandingController {
    private final GeneralUnderstandingService generalUnderstandingService;

    @PostMapping("/v1")
    public String generalUnderstanding(@Valid @RequestBody GeneralUnderstandingRequestParams generalUnderstandingRequestParams) {
        return generalUnderstandingService.generalUnderstanding(generalUnderstandingRequestParams);
    }
}
