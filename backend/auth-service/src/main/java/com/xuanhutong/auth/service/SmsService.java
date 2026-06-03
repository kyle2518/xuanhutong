package com.xuanhutong.auth.service;

public interface SmsService {
    void sendCode(String phone, String ipAddress);
    boolean verifyCode(String phone, String code);
}
