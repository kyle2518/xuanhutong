package com.xuanhutong.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuanhutong.auth.entity.SmsVerificationCode;
import com.xuanhutong.common.exception.BusinessException;
import com.xuanhutong.common.exception.ErrorCode;
import com.xuanhutong.auth.repository.SmsVerificationCodeRepository;
import com.xuanhutong.auth.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final SmsVerificationCodeRepository smsCodeRepository;

    @Value("${app.sms-mock:true}")
    private boolean smsMock;

    @Value("${app.sms-mock-code:123456}")
    private String mockCode;

    @Override
    public void sendCode(String phone, String ipAddress) {
        // Rate limit: only count UNUSED codes sent in last 60 seconds
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusSeconds(60);
        Long count = smsCodeRepository.selectCount(new LambdaQueryWrapper<SmsVerificationCode>()
                .eq(SmsVerificationCode::getPhone, phone)
                .eq(SmsVerificationCode::getUsed, 0)
                .ge(SmsVerificationCode::getCreatedAt, oneMinuteAgo));
        if (count > 0) {
            throw new BusinessException(ErrorCode.SMS_RATE_LIMIT);
        }

        String code = smsMock ? mockCode : generateCode();
        log.info("=== SMS to {}: verification code is [{}] ===", phone, code);

        SmsVerificationCode smsCode = new SmsVerificationCode();
        smsCode.setPhone(phone);
        smsCode.setCode(code);
        smsCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        smsCode.setIpAddress(ipAddress);
        smsCodeRepository.insert(smsCode);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        SmsVerificationCode smsCode = smsCodeRepository.selectOne(
                new LambdaQueryWrapper<SmsVerificationCode>()
                        .eq(SmsVerificationCode::getPhone, phone)
                        .eq(SmsVerificationCode::getUsed, 0)
                        .ge(SmsVerificationCode::getExpiresAt, LocalDateTime.now())
                        .orderByDesc(SmsVerificationCode::getCreatedAt)
                        .last("LIMIT 1"));
        if (smsCode == null) {
            log.warn("No valid code found for phone: {}", phone);
            return false;
        }
        boolean valid = smsCode.getCode().equals(code);
        if (valid) {
            smsCode.setUsed(1);
            smsCodeRepository.updateById(smsCode);
        }
        return valid;
    }

    private String generateCode() {
        return String.format("%06d", (int)(Math.random() * 1000000));
    }
}
