package com.xuanhutong.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sms_verification_codes")
public class SmsVerificationCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String code;
    private LocalDateTime expiresAt;
    private Integer used;
    private String ipAddress;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
