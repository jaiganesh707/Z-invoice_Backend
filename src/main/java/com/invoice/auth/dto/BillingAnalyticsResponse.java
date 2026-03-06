package com.invoice.auth.dto;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingAnalyticsResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal todayRevenue;
    private BigDecimal todayExpense;
    private BigDecimal todayProfit;
    private Long todayTotalTransactions;
    private BestMovingProduct bestMovingProduct;
    private List<ProductSalesStat> productSalesStats;
    private List<ExpenseStat> expenseStats;
    private List<StrategicAdvantage> strategicAdvantages;
    private List<AssetPrediction> predictions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategicAdvantage implements Serializable {
        private String assetName;
        private String advantageType; // e.g., "High Margin", "Fast Mover", "Growth Leader"
        private String description;
        private Double score; // 0-100
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetPrediction implements Serializable {
        private String assetName;
        private BigDecimal predictedNextPeriodRevenue;
        private Double confidence; // 0.0 - 1.0
        private String trend; // "UP", "DOWN", "STABLE"
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseStat implements Serializable {
        private String category;
        private BigDecimal amount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BestMovingProduct implements Serializable {
        private String name;
        private Integer quantity;
        private BigDecimal totalRevenue;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSalesStat implements Serializable {
        private String productName;
        private Integer quantity;
        private BigDecimal revenue;
    }
}
