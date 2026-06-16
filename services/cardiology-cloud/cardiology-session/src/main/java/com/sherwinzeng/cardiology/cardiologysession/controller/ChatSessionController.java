package com.sherwinzeng.cardiology.cardiologysession.controller;

import com.sherwinzeng.cardiology.cardiologysession.request.CreateChatSessionRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.services.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat/session")
@RequiredArgsConstructor
public class ChatSessionController {
    private final ChatSessionService chatSessionService;

    @PostMapping("/create")
    public String createSession(@Valid @RequestBody CreateChatSessionRequestParams createChatSessionRequestParams) {
        return chatSessionService.createSession(createChatSessionRequestParams);
    }
}
