package com.age.b2b.domain;

import com.age.b2b.domain.common.AdjustmentReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "inventory_logs")
public class InventoryLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    // [LAZY] 어떤 Lot의 재고가 변했는지 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private ProductLot productLot;

    // 변동 수량 (입고는 양수, 출고/폐기는 음수로 저장하거나, 로직에 따라 관리)
    private int changeQuantity;

    // 변경 후 최종 재고 수량 (나중에 역추적하기 쉽도록 스냅샷 저장)
    private int currentQuantity;

    // [핵심] 변동 사유 (입고, 출고, 분실, 파손 등) -> 여기서 Enum 사용!
    @Enumerated(EnumType.STRING)
    private AdjustmentReason reason;

    private String note; // 비고 (상세 사유 직접 입력)

    // --- 시간 설정 ---
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 변동 일시

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}