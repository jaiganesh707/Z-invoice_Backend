package com.invoice.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Table(name = "customers", indexes = {
    @Index(name = "idx_customer_user_id", columnList = "user_id"),
    @Index(name = "idx_customer_company", columnList = "companyName"),
    @Index(name = "idx_customer_active", columnList = "is_active"),
    @Index(name = "idx_customer_email", columnList = "email"),
    @Index(name = "idx_customer_type", columnList = "customerType")
})
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String contactNumber;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 10)
    private String pinCode;

    @Column(length = 20)
    private String gstin;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String customerType;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private java.math.BigDecimal outstandingBalance = java.math.BigDecimal.ZERO;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_pending_history", joinColumns = @JoinColumn(name = "customer_id"))
    @MapKeyColumn(name = "pending_date")
    @Column(name = "pending_amount")
    @Builder.Default
    private java.util.Map<java.time.LocalDate, java.math.BigDecimal> pendingHistory = new java.util.HashMap<>();

    @Builder.Default
    @Column(nullable = false, name = "is_active")
    private Boolean isActive = true;
}
