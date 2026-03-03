package com.invoice.auth.repository;

import com.invoice.auth.entity.Expense;
import com.invoice.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {
    List<Expense> findByUser(User user);

    List<Expense> findAllByUserOrderByCreatedAtDesc(User user);
}
