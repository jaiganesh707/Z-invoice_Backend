package com.invoice.auth.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodItemDto {
    private Integer id;
    private String name;
    private BigDecimal price;
    private String currency;
    private String description;
    private String imageUrl;
    private String uniqueCode;
}
