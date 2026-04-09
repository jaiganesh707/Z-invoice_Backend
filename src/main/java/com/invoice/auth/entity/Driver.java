package com.invoice.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Table(name = "drivers")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    private Integer age;

    @Column(length = 50)
    private String bikeNo;

    @Column(length = 100)
    private String licenseNumber;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String licensePhoto;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String driverPhoto;

    @Column(columnDefinition = "TEXT")
    private String address;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
