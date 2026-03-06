package com.invoice.auth.repository;

import com.invoice.auth.entity.Invoice;
import com.invoice.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends CrudRepository<Invoice, Integer> {
    @EntityGraph(attributePaths = { "items", "items.foodItem", "user" })
    List<Invoice> findAllByUserOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = { "items", "items.foodItem", "user" })
    List<Invoice> findAllByUserAndCreatedAtBetweenOrderByCreatedAtDesc(User user, java.time.LocalDateTime start,
            java.time.LocalDateTime end);

    @EntityGraph(attributePaths = { "items", "items.foodItem", "user" })
    List<Invoice> findAllByOrderByCreatedAtDesc();
}
