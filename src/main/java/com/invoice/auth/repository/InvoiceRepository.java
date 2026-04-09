package com.invoice.auth.repository;

import com.invoice.auth.entity.Invoice;
import com.invoice.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InvoiceRepository extends CrudRepository<Invoice, Integer> {
        @EntityGraph(attributePaths = { "items", "items.foodItem", "user" })
        List<Invoice> findAllByUserOrderByCreatedAtDesc(User user);

        @EntityGraph(attributePaths = { "items", "items.foodItem", "user" })
        List<Invoice> findAllByUserAndStatusOrderByCreatedAtDesc(User user, String status);

        @EntityGraph(attributePaths = { "items", "items.foodItem", "user" })
        List<Invoice> findAllByUserAndStatusInOrderByCreatedAtDesc(User user, List<String> statuses);

        @EntityGraph(attributePaths = { "items", "items.foodItem", "user" })
        List<Invoice> findAllByUserAndCreatedAtBetweenOrderByCreatedAtDesc(User user, java.time.LocalDateTime start,
                        java.time.LocalDateTime end);

        @EntityGraph(attributePaths = { "items", "items.foodItem", "user" })
        List<Invoice> findAllByOrderByCreatedAtDesc();

        @EntityGraph(attributePaths = { "items", "items.foodItem", "user", "createdBy" })
        @Query("SELECT i FROM Invoice i WHERE (i.user.id = :userId OR i.user.parentUser.id = :userId OR i.createdBy.id = :userId) ORDER BY i.createdAt DESC")
        List<Invoice> findAllByBusinessGroupId(@Param("userId") Integer userId);
    
        @EntityGraph(attributePaths = { "items", "items.foodItem", "user", "createdBy" })
        @Query("SELECT i FROM Invoice i WHERE (i.user.id = :userId OR i.user.parentUser.id = :userId OR i.createdBy.id = :userId) AND i.createdAt BETWEEN :start AND :end ORDER BY i.createdAt DESC")
        List<Invoice> findAllByBusinessGroupIdAndDateRange(@Param("userId") Integer userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
        @EntityGraph(attributePaths = { "items", "items.foodItem", "user", "createdBy" })
        List<Invoice> findAllByCreatedByOrderByCreatedAtDesc(User creator);

        @Query("SELECT i.user.id, COUNT(i), SUM(i.totalAmount) FROM Invoice i WHERE i.createdAt BETWEEN :start AND :end GROUP BY i.user.id")
        List<Object[]> getUserRevenueStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("SELECT ii.foodItem.name, SUM(ii.quantity), SUM(ii.price * ii.quantity) " +
                        "FROM InvoiceItem ii WHERE ii.invoice.user = :user GROUP BY ii.foodItem.name")
        List<Object[]> getProductSalesStats(@Param("user") User user);

        @EntityGraph(attributePaths = { "items", "items.foodItem", "user" })
        List<Invoice> findAllByAssignedDriver(User user);

        @EntityGraph(attributePaths = { "items", "items.foodItem", "user" })
        List<Invoice> findAllByAssignedDriverAndCreatedAtBetweenOrderByCreatedAtDesc(User user, LocalDateTime start, LocalDateTime end);

        @Query("SELECT SUM(i.balanceAmount) FROM Invoice i WHERE i.customer.id = :customerId AND i.status = 'APPROVED'")
        BigDecimal getCustomerOutstandingBalance(@Param("customerId") Integer customerId);
}
