package com.invoice.auth.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StakeholderPerformanceDto {
    private Integer userId;
    private String username;
    private BigDecimal revenue;
    private BigDecimal expense;
    private BigDecimal profit;
    private Long transactions;
}
