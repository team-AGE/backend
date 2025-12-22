package com.age.b2b.domain;

import com.age.b2b.domain.common.StockQuality;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "product_lots")
public class ProductLot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lot_id")
    private Long id;

    // [LAZY] 상품 정보가 필요할 때만 조회 (성능 최적화)
    @ManyToOne(fetch = FetchType.EAGER)
//    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private String lotNumber; // Lot 번호
    private int quantity;     // 현재 수량

    private LocalDate expiryDate;  // 유통기한
    private LocalDate inboundDate; // 입고일자

    @Enumerated(EnumType.STRING)
    private StockQuality stockQuality; // 재고상태 (정상, 주의 등)

    private String warehouseLocation;

    // --- 시간 설정 ---
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