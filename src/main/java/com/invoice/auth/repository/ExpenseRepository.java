package com.invoice.auth.repository;

import com.invoice.auth.entity.Expense;
import com.invoice.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {
    List<Expense> findByUser(User user);

    List<Expense> findAllByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT e.user.id, SUM(e.amount) FROM Expense e WHERE e.createdAt BETWEEN :start AND :end GROUP BY e.user.id")
    List<Object[]> getUserExpenseStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT e.itemName, SUM(e.amount) FROM Expense e WHERE e.user = :user GROUP BY e.itemName")
    List<Object[]> getExpenseCategoryStats(@Param("user") User user);
}
