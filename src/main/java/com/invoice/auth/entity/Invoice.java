package com.invoice.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_user_id", columnList = "user_id"),
    @Index(name = "idx_invoice_customer_id", columnList = "customer_id"),
    @Index(name = "idx_invoice_status", columnList = "status"),
    @Index(name = "idx_invoice_created_at", columnList = "created_at"),
    @Index(name = "idx_invoice_number", columnList = "invoiceNumber"),
    @Index(name = "idx_invoice_created_by", columnList = "created_by_id"),
    @Index(name = "idx_invoice_driver", columnList = "assigned_driver_id"),
    @Index(name = "idx_invoice_user_status", columnList = "user_id, status"),
    @Index(name = "idx_invoice_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_invoice_delivery", columnList = "deliveryStatus")
})
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled", "parentUser", "hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "user"})
    private Customer customer;

    @Column(unique = true, length = 50)
    private String invoiceNumber;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal previousBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InvoiceItem> items;

    @Builder.Default
    @Column(nullable = false)
    private String status = "PENDING_APPROVAL";

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_id")
    @JsonIgnoreProperties({"password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled", "hibernateLazyInitializer", "handler"})
    private User createdBy;

    @Column(name = "creator_name", length = 100)
    private String creatorName;

    @Column(name = "creator_employee_id")
    private Integer creatorEmployeeId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approved_by_id")
    @JsonIgnoreProperties({"password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled", "hibernateLazyInitializer", "handler"})
    private User approvedBy;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_driver_id")
    @JsonIgnoreProperties({"password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled", "parentUser", "hibernateLazyInitializer", "handler"})
    private User assignedDriver;

    @Column(length = 20)
    private String deliveryStatus; // E.g., ASSIGNED, IN_TRANSIT, DELIVERED

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean deliveryRequired = false;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal amountCollectedByDriver = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "TEXT")
    private String billingAddress;

    @Column(length = 50)
    private String customerGstin;

    @Column(columnDefinition = "TEXT")
    private String submissionNote;
}
