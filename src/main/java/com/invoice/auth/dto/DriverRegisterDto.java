package com.invoice.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverRegisterDto {
    private String name;
    private String email;
    private String password;
    private String contactNumber;
    private Integer age;
    private String address;
    private String licenseNumber;
}
