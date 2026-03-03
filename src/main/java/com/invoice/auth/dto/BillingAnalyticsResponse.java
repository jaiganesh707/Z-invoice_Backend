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
