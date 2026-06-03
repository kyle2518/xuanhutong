package com.xuanhutong.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "手机号不能为空")
    private String phone;
    private String code;       // SMS verification code
    private String password;   // password login
}
