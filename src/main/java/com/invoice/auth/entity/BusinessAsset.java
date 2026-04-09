package com.invoice.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Table(name = "business_assets", indexes = {
        @Index(name = "idx_asset_user_id", columnList = "user_id"),
        @Index(name = "idx_asset_name", columnList = "assetName"),
        @Index(name = "idx_asset_created_at", columnList = "created_at")
})
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({ "password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired",
            "enabled", "parentUser", "hibernateLazyInitializer", "handler" })
    private User user;

    @Column(nullable = false, length = 100)
    private String assetName;

    @Column(length = 500)
    private String description;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String assetImageUrl;

    @Column(length = 2083) // Max URL length for robustness
    private String targetUrl;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
