package com.invoice.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDetailsDto {
    private Integer userId;
    private String name;
    private String email;
    private String contactNumber;
    private Integer age;
    private String bikeNo;
    private String licenseNumber;
    private String licensePhoto;
    private String driverPhoto;
    private String address;
}
