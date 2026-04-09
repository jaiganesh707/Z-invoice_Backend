package com.invoice.auth.controller;

import com.invoice.auth.dto.StakeholderPerformanceDto;
import com.invoice.auth.dto.UserStatsResponse;
import java.util.List;

import com.invoice.auth.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<UserStatsResponse> getUserStats(
            @RequestParam(defaultValue = "day") String period) {
        return ResponseEntity.ok(analyticsService.getUserStats(period));
    }

    @GetMapping("/billing/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER', 'PRIME_USER')")
    public ResponseEntity<com.invoice.auth.dto.BillingAnalyticsResponse> getBillingAnalytics(
            @PathVariable Integer userId) {
        return ResponseEntity.ok(analyticsService.getUserBillingAnalytics(userId));
    }

    @GetMapping("/performance")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<List<StakeholderPerformanceDto>> getSuperadminPerformance(
            @RequestParam(defaultValue = "day") String period) {
        return ResponseEntity.ok(analyticsService.getSuperadminPerformance(period));
    }
}
