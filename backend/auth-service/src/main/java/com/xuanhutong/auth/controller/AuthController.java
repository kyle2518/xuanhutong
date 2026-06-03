package com.xuanhutong.auth.controller;

import com.xuanhutong.auth.dto.request.*;
import com.xuanhutong.common.dto.response.ApiResponse;
import com.xuanhutong.auth.dto.response.LoginResponse;
import com.xuanhutong.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-sms")
    public ApiResponse<Void> sendSms(@Valid @RequestBody SendSmsRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        authService.sendSmsCode(request, ip);
        return ApiResponse.success();
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse.UserInfo> getProfile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(authService.getProfile(userId));
    }

    @PutMapping("/me")
    public ApiResponse<LoginResponse.UserInfo> updateProfile(Authentication auth, @RequestBody UpdateProfileRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(authService.updateProfile(userId, request));
    }
}
