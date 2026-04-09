package com.invoice.auth.repository;

import com.invoice.auth.entity.Customer;
import com.invoice.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"pendingHistory"})
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Customer c WHERE c.user = :user AND (c.isActive = true OR c.isActive IS NULL)")
    List<Customer> findAllByUserAndIsActiveTrue(@org.springframework.data.repository.query.Param("user") User user);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Customer c WHERE c.isActive = true OR c.isActive IS NULL")
    List<Customer> findAllByIsActiveTrue();
}
