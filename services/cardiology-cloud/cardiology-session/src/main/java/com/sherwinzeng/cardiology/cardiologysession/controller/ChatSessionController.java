package com.sherwinzeng.cardiology.cardiologysession.controller;

import com.sherwinzeng.cardiology.cardiologysession.request.CreateChatSessionRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.request.PinChatSessionRequestParams;
import com.sherwinzeng.cardiology.cardiologysession.services.ChatSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/chat/session")
@RequiredArgsConstructor
public class ChatSessionController {
    private final ChatSessionService chatSessionService;

    @PostMapping("/create")
    public String createSession(@Valid @RequestBody CreateChatSessionRequestParams createChatSessionRequestParams) {
        return chatSessionService.createSession(createChatSessionRequestParams);
    }

    @GetMapping("/list/v1")
    public String listSessions(
            @RequestParam("uid") @NotBlank(message = "uid 不能为空") String uid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return chatSessionService.listSessions(uid, page, pageSize, keyword);
    }

    @DeleteMapping("/v1")
    public String deleteSession(
            @RequestParam("uid") @NotBlank(message = "uid 不能为空") String uid,
            @RequestParam("session") @NotBlank(message = "session 不能为空") String session
    ) {
        return chatSessionService.deleteSession(uid, session);
    }

    @PostMapping("/pin/v1")
    public String pinSession(@Valid @RequestBody PinChatSessionRequestParams params) {
        return chatSessionService.pinSession(params);
    }
}
