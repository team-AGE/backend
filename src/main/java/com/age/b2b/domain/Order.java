package com.age.b2b.domain;

import com.age.b2b.domain.common.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "orders")
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(unique = true)
    private String orderNumber; // 발주번호 (예: 20251212-001)

    // [LAZY] 주문 정보를 볼 때 고객 정보까지 다 가져올 필요 없음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // 발주상태 (출고전, 배송완료 등)

    @Column(name= "total_amount", nullable = false)
    private int totalAmount; // 총 금액

    @Embedded
    private DeliveryInfo deliveryInfo; // 배송지 정보 묶음

    private LocalDateTime deliveryCompletedAt; // 배송완료일자

    // [Cascade ALL] 주문을 저장하면 상세 상품들도 같이 저장, 주문 삭제하면 같이 삭제
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private String cancelReason;  // 취소 사유 (단순변심, 배송지연 등)
    private String cancelDetail;  // 취소 상세 사유 (사용자 입력 텍스트)

    private LocalDateTime canceledAt; // 취소일자
    private LocalDateTime returnedAt;

    // --- 시간 설정 ---
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 발주일자
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 상태처리일자

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

    // 연관관계 편의 메서드
    public void addOrderItem(OrderItem item) {
        this.orderItems.add(item);
        item.setOrder(this);
    }

    // 반품 관련 필드
    private String returnReason;  // 반품 사유
    private String returnDetail;  // 반품 상세 사유

}