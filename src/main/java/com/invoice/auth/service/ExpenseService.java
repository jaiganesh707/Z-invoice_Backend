package com.invoice.auth.service;

import com.invoice.auth.entity.Expense;
import com.invoice.auth.entity.User;
import com.invoice.auth.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenseRepository;

    public List<Expense> getExpensesByUser(@NonNull User user) {
        return expenseRepository.findByUser(user);
    }

    public @NonNull Expense saveExpense(@NonNull Expense expense) {
        return expenseRepository.save(expense);
    }

    public void deleteExpense(@NonNull Integer id) {
        expenseRepository.deleteById(id);
    }
}
