package com.invoice.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private Integer id;
    private String companyName;
    private String customerName;
    private String contactNumber;
    private String address;
    private String city;
    private String state;
    private String pinCode;
    private String gstin;
    private String email;
    private String customerType;
    private java.math.BigDecimal outstandingBalance;
    private java.util.Map<java.time.LocalDate, java.math.BigDecimal> pendingHistory;
}
