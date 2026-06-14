package com.sherwinzeng.cardiology.cardiologysession.controller;

import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologysession.response.ChatMessageResponse;
import com.sherwinzeng.cardiology.cardiologysession.services.ChatMessageService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/v1")
    public BaseResponse<List<ChatMessageResponse>> listMessages(
            @RequestParam @NotBlank(message = "uid 不能为空") String uid,
            @RequestParam @NotBlank(message = "session 不能为空") String session
    ) {
        return BaseResponse.success(chatMessageService.listBySession(uid, session));
    }
}
