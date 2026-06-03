package com.xuanhutong.auth.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;
    private String clinicName;
    private String signatureImage;
}
