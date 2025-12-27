package com.age.b2b.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "shipments")
public class Shipment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_id")
    private Long id;

    @Column(unique = true)
    private String shipmentNumber; // 출고번호 (예: SHP-20251224-001)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private LocalDateTime shippedDate; // 출고일시

    private String courier; // 택배사
    private String trackingNumber; // 운송장번호

    @PrePersist
    public void prePersist() {
        this.shippedDate = LocalDateTime.now();
    }
}