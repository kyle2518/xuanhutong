package com.xuanhutong.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    PHONE_ALREADY_EXISTS(1001, "手机号已注册"),
    SMS_CODE_ERROR(1002, "验证码错误"),
    SMS_CODE_EXPIRED(1003, "验证码已过期"),
    SMS_RATE_LIMIT(1004, "发送验证码过于频繁"),
    USER_NOT_FOUND(1005, "用户不存在"),
    PATIENT_NOT_FOUND(2001, "病人不存在"),
    PRESCRIPTION_NOT_FOUND(3001, "药方不存在"),
    HERB_NOT_FOUND(3002, "药材不存在"),
    INVENTORY_NOT_FOUND(3003, "库存记录不存在"),
    APPOINTMENT_NOT_FOUND(4001, "预约不存在"),
    APPOINTMENT_TIME_CONFLICT(4002, "预约时间冲突"),
    FILE_UPLOAD_FAILED(5001, "文件上传失败"),
    PDF_GENERATION_FAILED(5002, "PDF生成失败"),
    INTERNAL_ERROR(5000, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
