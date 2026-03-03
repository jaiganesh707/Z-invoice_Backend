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

        public BillingAnalyticsResponse getUserBillingAnalytics(Integer userId) {
                User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
                List<Invoice> userInvoices = invoiceRepository.findAllByUserOrderByCreatedAtDesc(user);
                List<Expense> userExpenses = expenseRepository.findAllByUserOrderByCreatedAtDesc(user);

                LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
                LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

                BigDecimal todayRevenue = BigDecimal.ZERO;
                BigDecimal todayExpenseAmount = BigDecimal.ZERO;
                long todayTransactions = 0;
                Map<String, BillingAnalyticsResponse.ProductSalesStat> productStats = new HashMap<>();
                Map<String, BigDecimal> expenseCategoryMap = new HashMap<>();

                for (Invoice invoice : userInvoices) {
                        try {
                                if (invoice == null || invoice.getCreatedAt() == null) {
                                        log.warn("Skipping null invoice or invoice with null createdAt: {}", invoice);
                                        continue;
                                }

                                if (invoice.getCreatedAt().isAfter(startOfToday)
                                                && invoice.getCreatedAt().isBefore(endOfToday)) {

                                        if (invoice.getTotalAmount() != null) {
                                                todayRevenue = todayRevenue.add(invoice.getTotalAmount());
                                        }
                                        todayTransactions++;

                                        if (invoice.getItems() != null) {
                                                for (InvoiceItem item : invoice.getItems()) {
                                                        if (item == null || item.getFoodItem() == null) {
                                                                log.warn("Skipping null item or item with null foodItem in invoice ID: {}",
                                                                                invoice.getId());
                                                                continue;
                                                        }

                                                        String productName = item.getFoodItem().getName();
                                                        if (productName == null)
                                                                productName = "Unknown Product";

                                                        BillingAnalyticsResponse.ProductSalesStat stat = productStats
                                                                        .getOrDefault(
                                                                                        productName,
                                                                                        BillingAnalyticsResponse.ProductSalesStat
                                                                                                        .builder()
                                                                                                        .productName(productName)
                                                                                                        .quantity(0)
                                                                                                        .revenue(BigDecimal.ZERO)
                                                                                                        .build());

                                                        int quantity = item.getQuantity() != null ? item.getQuantity()
                                                                        : 0;
                                                        BigDecimal price = item.getPrice() != null ? item.getPrice()
                                                                        : BigDecimal.ZERO;

                                                        stat.setQuantity(stat.getQuantity() + quantity);
                                                        stat.setRevenue(stat.getRevenue().add(
                                                                        price.multiply(BigDecimal.valueOf(quantity))));
                                                        productStats.put(productName, stat);
                                                }
                                        }
                                }
                        } catch (Exception e) {
                                log.error("Error processing invoice ID: {}. Error: {}",
                                                (invoice != null ? invoice.getId() : "null"), e.getMessage());
                        }
                }

                for (Expense expense : userExpenses) {
                        try {
                                if (expense == null || expense.getCreatedAt() == null)
                                        continue;

                                if (expense.getCreatedAt().isAfter(startOfToday)
                                                && expense.getCreatedAt().isBefore(endOfToday)) {
                                        BigDecimal amount = expense.getAmount() != null ? expense.getAmount()
                                                        : BigDecimal.ZERO;
                                        todayExpenseAmount = todayExpenseAmount.add(amount);

                                        String itemName = expense.getItemName() != null ? expense.getItemName()
                                                        : "Other Expense";
                                        expenseCategoryMap.put(itemName, expenseCategoryMap
                                                        .getOrDefault(itemName, BigDecimal.ZERO).add(amount));
                                }
                        } catch (Exception e) {
                                log.error("Error processing expense ID: {}. Error: {}",
                                                (expense != null ? expense.getId() : "null"), e.getMessage());
                        }
                }

                List<BillingAnalyticsResponse.ProductSalesStat> statsList = new ArrayList<>(productStats.values());
                statsList.sort((a, b) -> b.getQuantity().compareTo(a.getQuantity()));

                List<BillingAnalyticsResponse.ExpenseStat> expenseStats = new ArrayList<>();
                for (Map.Entry<String, BigDecimal> entry : expenseCategoryMap.entrySet()) {
                        expenseStats.add(new BillingAnalyticsResponse.ExpenseStat(entry.getKey(), entry.getValue()));
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
                                .todayRevenue(todayRevenue)
                                .todayExpense(todayExpenseAmount)
                                .todayProfit(todayRevenue.subtract(todayExpenseAmount))
                                .todayTotalTransactions(todayTransactions)
                                .bestMovingProduct(bestProduct)
                                .productSalesStats(statsList)
                                .expenseStats(expenseStats)
                                .build();
        }

        public List<StakeholderPerformanceDto> getSuperadminPerformance() {
                Iterable<User> usersIterable = userRepository.findAll();
                List<StakeholderPerformanceDto> performanceResults = new ArrayList<>();

                LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
                LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

                for (User user : usersIterable) {
                        if (user.getRole() == RoleEnum.ROLE_SUPER_ADMIN)
                                continue;

                        BigDecimal rev = BigDecimal.ZERO;
                        BigDecimal exp = BigDecimal.ZERO;
                        long trans = 0;

                        List<Invoice> invoices = invoiceRepository.findAllByUserOrderByCreatedAtDesc(user);
                        for (Invoice inv : invoices) {
                                if (inv.getCreatedAt() != null && inv.getCreatedAt().isAfter(startOfToday)
                                                && inv.getCreatedAt().isBefore(endOfToday)) {
                                        if (inv.getTotalAmount() != null)
                                                rev = rev.add(inv.getTotalAmount());
                                        trans++;
                                }
                        }

                        List<Expense> expenses = expenseRepository.findAllByUserOrderByCreatedAtDesc(user);
                        for (Expense e : expenses) {
                                if (e.getCreatedAt() != null && e.getCreatedAt().isAfter(startOfToday)
                                                && e.getCreatedAt().isBefore(endOfToday)) {
                                        if (e.getAmount() != null)
                                                exp = exp.add(e.getAmount());
                                }
                        }

                        performanceResults.add(StakeholderPerformanceDto.builder()
                                        .userId(user.getId())
                                        .username(user.getUsername())
                                        .revenue(rev)
                                        .expense(exp)
                                        .profit(rev.subtract(exp))
                                        .transactions(trans)
                                        .build());
                }

                // Sort by profit descending
                performanceResults.sort((a, b) -> b.getProfit().compareTo(a.getProfit()));
                return performanceResults;
        }
}
