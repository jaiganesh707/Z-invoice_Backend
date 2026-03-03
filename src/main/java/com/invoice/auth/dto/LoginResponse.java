package com.invoice.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String refreshToken;
    private long expiresIn;
    private Integer id;
    private String username;
    private String email;
    private String role;
    private String contactNumber;
}
