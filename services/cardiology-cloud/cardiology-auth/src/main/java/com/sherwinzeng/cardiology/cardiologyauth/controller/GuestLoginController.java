package com.sherwinzeng.cardiology.cardiologyauth.controller;

import com.sherwinzeng.cardiology.cardiologyauth.request.GuestLoginRequestParams;
import com.sherwinzeng.cardiology.cardiologyauth.services.GuestLoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/auth/guest/login")
@RequiredArgsConstructor
public class GuestLoginController {
    private final GuestLoginService guestLoginService;

    @PostMapping("/v1")
    public String guestLogin(@Valid @RequestBody GuestLoginRequestParams guestLoginRequestParams) {
        return guestLoginService.guestLogin(guestLoginRequestParams);
    }
}
