package com.invoice.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomTaxDto {
    private Integer id;
    private Integer userId;
    private String name;
    private BigDecimal percentage;
    private String description;
    
    @JsonProperty("isActive")
    private boolean isActive;
}
