package com.xuanhutong.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendSmsRequest {
    @NotBlank(message = "手机号不能为空")
    private String phone;
}
