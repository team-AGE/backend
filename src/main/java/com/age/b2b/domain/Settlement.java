package com.age.b2b.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "settlements")
public class Settlement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @Column(nullable = false)
    private String settlementNumber; // 정산번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    private String settlementMonth; // 정산기준월 (YYYY-MM)
    private Long totalAmount;       // 정산 금액 합계
    private String status;          // 정산상태 (정산완료, 미정산)

    // --- 시간 설정 ---
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 정산 생성일

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }
}
