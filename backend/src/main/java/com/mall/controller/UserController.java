package com.mall.controller;

import com.mall.common.Result;
import com.mall.model.dto.LoginRequest;
import com.mall.model.dto.RegisterRequest;
import com.mall.model.dto.SendCodeRequest;
import com.mall.service.UserService;
import com.mall.service.VerificationCodeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final VerificationCodeService verificationCodeService;

    public UserController(UserService userService, VerificationCodeService verificationCodeService) {
        this.userService = userService;
        this.verificationCodeService = verificationCodeService;
    }

    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        verificationCodeService.sendRegisterCode(request.getEmail());
        return Result.ok();
    }

    @PostMapping("/register")
    public Result<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        String token = userService.register(request);
        return Result.ok(Map.of("token", token));
    }

    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request);
        return Result.ok(Map.of("token", token));
    }
}
