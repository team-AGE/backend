package com.age.b2b.domain;

import com.age.b2b.domain.common.ProductStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "products")
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String productCode; // 상품코드

    @Column(nullable = false)
    private String name; // 상품명

    private int consumerPrice; // 소비자가
    private int supplyPrice;   // 공급가
    private int costPrice;     // 제조원가
    private String origin;     // 원산지

    @Column(name = "prod_expiry_date")
    private LocalDate expiryDate;

    @Lob // 긴 텍스트
    private String description; // 상품설명

    @Enumerated(EnumType.STRING)
    private ProductStatus status; // 상품상태

    // --- 시간 설정 (BaseTimeEntity 미사용) ---
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}