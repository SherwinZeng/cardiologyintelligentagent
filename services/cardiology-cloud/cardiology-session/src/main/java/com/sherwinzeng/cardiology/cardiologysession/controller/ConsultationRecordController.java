package com.sherwinzeng.cardiology.cardiologysession.controller;

import com.sherwinzeng.cardiology.cardiologycloudcommonutils.auth.AuthHeaders;
import com.sherwinzeng.cardiology.cardiologysession.services.ConsultationRecordService;
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
@RequestMapping("/chat/record")
@RequiredArgsConstructor
public class ConsultationRecordController {

    private final ConsultationRecordService consultationRecordService;

    @GetMapping("/list/v1")
    public String listRecords(
            @RequestParam("uid") @NotBlank(message = "uid 不能为空") String uid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "urgency", required = false) String urgency,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestHeader(value = AuthHeaders.USER_TYPE, required = false) String userType,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUid
    ) {
        return consultationRecordService.listRecords(
                uid,
                page,
                pageSize,
                urgency,
                keyword,
                startDate,
                endDate,
                userType,
                authenticatedUid
        );
    }
}
