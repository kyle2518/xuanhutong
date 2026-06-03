package com.xuanhutong.auth.service;

import com.xuanhutong.auth.dto.request.*;
import com.xuanhutong.auth.dto.response.LoginResponse;

public interface AuthService {
    void sendSmsCode(SendSmsRequest request, String ipAddress);
    LoginResponse login(LoginRequest request);
    LoginResponse register(RegisterRequest request);
    LoginResponse.UserInfo getProfile(Long userId);
    LoginResponse.UserInfo updateProfile(Long userId, UpdateProfileRequest request);
}
