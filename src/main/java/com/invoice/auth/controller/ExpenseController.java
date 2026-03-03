package com.invoice.auth.controller;

import com.invoice.auth.entity.Expense;
import com.invoice.auth.entity.User;
import com.invoice.auth.service.ExpenseService;
import com.invoice.auth.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.lang.NonNull;
import java.util.List;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ExpenseController {
    private final ExpenseService expenseService;
    private final AuthenticationService authenticationService;

    @SuppressWarnings("null")
    @GetMapping
    public ResponseEntity<List<Expense>> getMyExpenses(
            @RequestParam(required = false) Integer userId,
            @AuthenticationPrincipal User requester) {
        User targetUser = requester;

        if (userId != null) {
            if (requester.getRole() != com.invoice.auth.entity.RoleEnum.ROLE_SUPER_ADMIN) {
                throw new RuntimeException("Access denied: Only Super Admin can view other users' expenses");
            }
            User user = authenticationService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));
            targetUser = user;
        }

        return ResponseEntity.ok(expenseService.getExpensesByUser(targetUser));
    }

    @SuppressWarnings("null")
    @PostMapping
    public ResponseEntity<Expense> addExpense(
            @RequestBody Expense expense,
            @AuthenticationPrincipal User user) {
        expense.setUser(user);
        return ResponseEntity.ok(expenseService.saveExpense(expense));
    }

    @SuppressWarnings("null")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable @NonNull Integer id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}
