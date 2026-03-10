package com.invoice.auth.service;

import com.invoice.auth.dto.BillingAnalyticsResponse;
import com.invoice.auth.dto.StakeholderPerformanceDto;
import com.invoice.auth.dto.UserStatsResponse;
import com.invoice.auth.entity.Expense;
import com.invoice.auth.entity.Invoice;
import com.invoice.auth.entity.InvoiceItem;
import com.invoice.auth.entity.RoleEnum;
import com.invoice.auth.entity.User;
import com.invoice.auth.repository.ExpenseRepository;
import com.invoice.auth.repository.InvoiceRepository;
import com.invoice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class AnalyticsService {
        private final UserRepository userRepository;
        private final InvoiceRepository invoiceRepository;
        private final ExpenseRepository expenseRepository;

        public UserStatsResponse getUserStats(String period) {
                try {
                        Iterable<User> usersIterable = userRepository.findAll();
                        List<User> users = new ArrayList<>();
                        if (usersIterable != null) {
                                for (User u : usersIterable) {
                                        if (u != null)
                                                users.add(u);
                                }
                        }

                        long totalUsers = (long) users.size();
                        DateTimeFormatter formatter = getDateTimeFormatter(period);
                        Map<String, Long> usersByPeriod = new TreeMap<>();

                        for (User u : users) {
                                if (u.getCreatedAt() != null) {
                                        try {
                                                String label = u.getCreatedAt().format(formatter);
                                                usersByPeriod.put(label, usersByPeriod.getOrDefault(label, 0L) + 1L);
                                        } catch (Exception fmtEx) {
                                                // Skip malformed dates
                                        }
                                }
                        }

                        List<UserStatsResponse.DataPoint> chartData = new ArrayList<>();
                        for (Map.Entry<String, Long> entry : usersByPeriod.entrySet()) {
                                chartData.add(new UserStatsResponse.DataPoint(entry.getKey(), entry.getValue()));
                        }

                        return new UserStatsResponse(totalUsers, chartData);
                } catch (Exception e) {
                        e.printStackTrace();
                        return new UserStatsResponse(0L, new ArrayList<>());
                }
        }

        private DateTimeFormatter getDateTimeFormatter(String period) {
                return switch (period.toLowerCase()) {
                        case "year" -> DateTimeFormatter.ofPattern("yyyy");
                        case "month" -> DateTimeFormatter.ofPattern("yyyy-MM");
                        default -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
                };
        }

        @org.springframework.transaction.annotation.Transactional(readOnly = true)
        public BillingAnalyticsResponse getUserBillingAnalytics(Integer userId) {
                User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

                LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
                LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

                BigDecimal todayRevenue = BigDecimal.ZERO;
                BigDecimal todayExpenseAmount = BigDecimal.ZERO;
                long todayTransactions = 0;

                // 1. Get Today's Stats via efficient aggregate queries
                List<Object[]> revenueStats = invoiceRepository.getUserRevenueStats(startOfToday, endOfToday);
                for (Object[] row : revenueStats) {
                        if (row[0].equals(userId)) {
                                todayTransactions = (Long) row[1];
                                todayRevenue = (BigDecimal) row[2];
                                break;
                        }
                }

                List<Object[]> expenseStatsToday = expenseRepository.getUserExpenseStats(startOfToday, endOfToday);
                for (Object[] row : expenseStatsToday) {
                        if (row[0].equals(userId)) {
                                todayExpenseAmount = (BigDecimal) row[1];
                                break;
                        }
                }

                // 2. Get Historical Product Stats via aggregation
                List<BillingAnalyticsResponse.ProductSalesStat> statsList = new ArrayList<>();
                List<Object[]> productStatsRows = invoiceRepository.getProductSalesStats(user);
                for (Object[] row : productStatsRows) {
                        statsList.add(BillingAnalyticsResponse.ProductSalesStat.builder()
                                        .productName((String) row[0])
                                        .quantity(((Long) row[1]).intValue())
                                        .revenue((BigDecimal) row[2])
                                        .build());
                }
                statsList.sort((a, b) -> b.getQuantity().compareTo(a.getQuantity()));

                // 3. Get Historical Expense Category Stats via aggregation
                List<BillingAnalyticsResponse.ExpenseStat> expenseStatsList = new ArrayList<>();
                List<Object[]> categoryStatsRows = expenseRepository.getExpenseCategoryStats(user);
                for (Object[] row : categoryStatsRows) {
                        expenseStatsList.add(
                                        new BillingAnalyticsResponse.ExpenseStat((String) row[0], (BigDecimal) row[1]));
                }

                BillingAnalyticsResponse.BestMovingProduct bestProduct = null;
                if (!statsList.isEmpty()) {
                        BillingAnalyticsResponse.ProductSalesStat top = statsList.get(0);
                        bestProduct = BillingAnalyticsResponse.BestMovingProduct.builder()
                                        .name(top.getProductName())
                                        .quantity(top.getQuantity())
                                        .totalRevenue(top.getRevenue())
                                        .build();
                }

                return BillingAnalyticsResponse.builder()
                                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                                .todayExpense(todayExpenseAmount != null ? todayExpenseAmount : BigDecimal.ZERO)
                                .todayProfit((todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                                                .subtract(todayExpenseAmount != null ? todayExpenseAmount
                                                                : BigDecimal.ZERO))
                                .todayTotalTransactions(todayTransactions)
                                .bestMovingProduct(bestProduct)
                                .productSalesStats(statsList)
                                .expenseStats(expenseStatsList)
                                .strategicAdvantages(calculateStrategicAdvantages(statsList))
                                .predictions(calculatePredictions(statsList, user))
                                .build();
        }

        private List<BillingAnalyticsResponse.StrategicAdvantage> calculateStrategicAdvantages(
                        List<BillingAnalyticsResponse.ProductSalesStat> stats) {
                List<BillingAnalyticsResponse.StrategicAdvantage> advantages = new ArrayList<>();
                if (stats == null)
                        return advantages;

                for (BillingAnalyticsResponse.ProductSalesStat stat : stats) {
                        if (stat.getQuantity() > 2) { // Lowered from 10
                                advantages.add(BillingAnalyticsResponse.StrategicAdvantage.builder()
                                                .assetName(stat.getProductName())
                                                .advantageType("Fast Mover")
                                                .description("High liquidity asset with consistent demand")
                                                .score(85.0)
                                                .build());
                        }
                        if (stat.getRevenue().compareTo(BigDecimal.valueOf(100)) > 0) { // Lowered from 1000
                                advantages.add(BillingAnalyticsResponse.StrategicAdvantage.builder()
                                                .assetName(stat.getProductName())
                                                .advantageType("Revenue Driver")
                                                .description("Core contributor to net node revenue")
                                                .score(92.0)
                                                .build());
                        }
                }
                return advantages;
        }

        private List<BillingAnalyticsResponse.AssetPrediction> calculatePredictions(
                        List<BillingAnalyticsResponse.ProductSalesStat> stats, User user) {
                List<BillingAnalyticsResponse.AssetPrediction> predictions = new ArrayList<>();
                Random random = new Random();

                if (stats == null || stats.isEmpty()) {
                        // Fallback: Predict based on global outlook if no sales yet
                        predictions.add(BillingAnalyticsResponse.AssetPrediction.builder()
                                        .assetName("Market Outlook")
                                        .predictedNextPeriodRevenue(BigDecimal.valueOf(1000 + random.nextInt(5000)))
                                        .confidence(0.65)
                                        .trend("UP")
                                        .build());
                        return predictions;
                }

                for (BillingAnalyticsResponse.ProductSalesStat stat : stats) {
                        BigDecimal multiplier = BigDecimal.valueOf(1.05 + (random.nextDouble() * 0.1));
                        predictions.add(BillingAnalyticsResponse.AssetPrediction.builder()
                                        .assetName(stat.getProductName())
                                        .predictedNextPeriodRevenue(stat.getRevenue().multiply(multiplier))
                                        .confidence(0.75 + (random.nextDouble() * 0.15))
                                        .trend(random.nextBoolean() ? "UP" : "STABLE")
                                        .build());
                }
                return predictions;
        }

        public List<StakeholderPerformanceDto> getSuperadminPerformance() {
                LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
                LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

                // 1. Fetch all revenue stats for today in one query
                Map<Integer, Object[]> revenueMap = new HashMap<>();
                List<Object[]> revenueRows = invoiceRepository.getUserRevenueStats(startOfToday, endOfToday);
                for (Object[] row : revenueRows) {
                        revenueMap.put((Integer) row[0], row);
                }

                // 2. Fetch all expense stats for today in one query
                Map<Integer, BigDecimal> expenseMap = new HashMap<>();
                List<Object[]> expenseRows = expenseRepository.getUserExpenseStats(startOfToday, endOfToday);
                for (Object[] row : expenseRows) {
                        expenseMap.put((Integer) row[0], (BigDecimal) row[1]);
                }

                // 3. Get all users and build response list
                Iterable<User> usersIterable = userRepository.findAll();
                List<StakeholderPerformanceDto> performanceResults = new ArrayList<>();

                for (User user : usersIterable) {
                        if (user.getRole() == RoleEnum.ROLE_SUPER_ADMIN)
                                continue;

                        Object[] revStat = revenueMap.get(user.getId());
                        long transactions = revStat != null ? (Long) revStat[1] : 0;
                        BigDecimal revenue = revStat != null ? (BigDecimal) revStat[2] : BigDecimal.ZERO;

                        BigDecimal expense = expenseMap.getOrDefault(user.getId(), BigDecimal.ZERO);

                        performanceResults.add(StakeholderPerformanceDto.builder()
                                        .userId(user.getId())
                                        .username(user.getUsername())
                                        .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                                        .expense(expense != null ? expense : BigDecimal.ZERO)
                                        .profit((revenue != null ? revenue : BigDecimal.ZERO)
                                                        .subtract(expense != null ? expense : BigDecimal.ZERO))
                                        .transactions(transactions)
                                        .build());
                }

                // Sort by profit descending
                performanceResults.sort((a, b) -> b.getProfit().compareTo(a.getProfit()));
                return performanceResults;
        }
}
