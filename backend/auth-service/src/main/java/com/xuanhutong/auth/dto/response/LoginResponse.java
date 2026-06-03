package com.xuanhutong.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String name;
        private String phone;
        private String role;
        private String clinicName;
        private String avatarUrl;
        private String signatureImageUrl;
    }
}
