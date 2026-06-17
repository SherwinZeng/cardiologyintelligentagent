package com.sherwinzeng.cardiology.cardiologysession.controller;

import com.sherwinzeng.cardiology.cardiologycloudcommonutils.auth.AuthHeaders;
import com.sherwinzeng.cardiology.cardiologysession.services.ChatMessageService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/v1")
    public String listMessages(
            @RequestParam("uid") @NotBlank(message = "uid 不能为空") String uid,
            @RequestParam("session") @NotBlank(message = "session 不能为空") String session,
            @RequestParam(value = "beforeId", required = false) Long beforeId,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = AuthHeaders.USER_TYPE, required = false) String userType,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUid
    ) {
        return chatMessageService.listMessages(uid, session, beforeId, pageSize, userType, authenticatedUid);
    }
}
