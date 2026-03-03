package com.invoice.auth.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitStatsResponse {
    private BigDecimal totalSales;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;
    private List<DataPoint> chartData;

    @Data
    @AllArgsConstructor
    public static class DataPoint {
        private String label;
        private BigDecimal sales;
        private BigDecimal expenses;
        private BigDecimal profit;
    }
}
