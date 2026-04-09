package com.invoice.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Table(name = "delivery_orders", indexes = {
    @Index(name = "idx_delivery_ord_customer", columnList = "customer_id"),
    @Index(name = "idx_delivery_ord_driver", columnList = "driver_id"),
    @Index(name = "idx_delivery_ord_status", columnList = "status"),
    @Index(name = "idx_delivery_ord_created", columnList = "created_at")
})
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    private User customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", referencedColumnName = "id")
    private Driver driver;

    @Column(length = 200)
    private String shopName;

    @Column(columnDefinition = "TEXT")
    private String shopDetails;

    @Column(columnDefinition = "TEXT")
    private String pickupMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
