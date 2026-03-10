package com.invoice.auth.repository;

import com.invoice.auth.entity.Invoice;
import com.invoice.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
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

        @Query("SELECT i.user.id, COUNT(i), SUM(i.totalAmount) FROM Invoice i WHERE i.createdAt BETWEEN :start AND :end GROUP BY i.user.id")
        List<Object[]> getUserRevenueStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("SELECT ii.foodItem.name, SUM(ii.quantity), SUM(ii.price * ii.quantity) " +
                        "FROM InvoiceItem ii WHERE ii.invoice.user = :user GROUP BY ii.foodItem.name")
        List<Object[]> getProductSalesStats(@Param("user") User user);
}
