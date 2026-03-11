package com.invoice.auth.dto;

import com.invoice.auth.entity.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserDto {
    private String email;
    private String password;
    private String username;
    private String contactNumber;
    private RoleEnum role;
    private String upiId;
    private String payeeName;
    private String currency;
}
