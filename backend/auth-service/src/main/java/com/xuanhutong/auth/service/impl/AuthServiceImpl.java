package com.xuanhutong.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuanhutong.auth.dto.request.*;
import com.xuanhutong.auth.dto.response.LoginResponse;
import com.xuanhutong.common.entity.User;
import com.xuanhutong.common.exception.BusinessException;
import com.xuanhutong.common.exception.ErrorCode;
import com.xuanhutong.auth.repository.UserRepository;
import com.xuanhutong.common.security.JwtTokenProvider;
import com.xuanhutong.auth.service.AuthService;
import com.xuanhutong.auth.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final SmsService smsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void sendSmsCode(SendSmsRequest request, String ipAddress) {
        smsService.sendCode(request.getPhone(), ipAddress);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user;

        if (StringUtils.hasText(request.getCode())) {
            // SMS code login
            if (!smsService.verifyCode(request.getPhone(), request.getCode())) {
                throw new BusinessException(ErrorCode.SMS_CODE_ERROR);
            }
            user = userRepository.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, request.getPhone()));
        } else if (StringUtils.hasText(request.getPassword())) {
            // Password login
            user = userRepository.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, request.getPhone()));
            if (user == null || user.getPassword() == null
                    || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BusinessException(ErrorCode.SMS_CODE_ERROR.getCode(), "手机号或密码错误");
            }
        } else {
            throw new BusinessException(400, "请输入验证码或密码");
        }

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getPhone(), user.getRole());
        log.info("User {} ({}) logged in successfully", user.getName(), user.getPhone());
        return LoginResponse.builder()
                .token(token)
                .user(buildUserInfo(user))
                .build();
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        // Check for existing user FIRST before verifying code,
        // so the user gets a clear "already registered" message
        User existing = userRepository.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.getPhone()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
        if (!smsService.verifyCode(request.getPhone(), request.getCode())) {
            throw new BusinessException(ErrorCode.SMS_CODE_ERROR);
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setName(request.getName());
        user.setRole("DOCTOR");
        user.setClinicName(request.getClinicName());
        user.setStatus(1);

        // Always set password if provided
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        } else {
            // Set a default random password if none provided (user can set later)
            user.setPassword(passwordEncoder.encode("123456"));
        }

        userRepository.insert(user);
        log.info("New user registered: {} ({})", user.getName(), user.getPhone());

        String token = jwtTokenProvider.generateToken(user.getId(), user.getPhone(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .user(buildUserInfo(user))
                .build();
    }

    @Override
    public LoginResponse.UserInfo getProfile(Long userId) {
        User user = userRepository.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        return buildUserInfo(user);
    }

    @Override
    public LoginResponse.UserInfo updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if (request.getName() != null) user.setName(request.getName());
        if (request.getClinicName() != null) user.setClinicName(request.getClinicName());
        if (request.getSignatureImage() != null) user.setSignatureImageUrl(request.getSignatureImage());
        userRepository.updateById(user);
        return buildUserInfo(user);
    }

    private LoginResponse.UserInfo buildUserInfo(User user) {
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole())
                .clinicName(user.getClinicName())
                .avatarUrl(user.getAvatarUrl())
                .signatureImageUrl(user.getSignatureImageUrl())
                .build();
    }
}
