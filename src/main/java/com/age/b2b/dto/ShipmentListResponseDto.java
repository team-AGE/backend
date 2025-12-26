package com.age.b2b.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentListResponseDto {
    private Long shipmentId;        // 출고 PK
    private String shipmentNumber;  // 출고 번호
    private String orderNumber;     // 발주 번호
    private String orderDate;       // 발주 일자
    private String productCode;     // 대표 상품 코드
    private String productName;     // 대표 상품명
    private int quantity;           // 총 수량
    private String lotNumber;       // LOT 번호
    private String expiryDate;      // 유통기한
    private String stockStatus;     // 재고상태
    private String origin;          // 원산지
    private String address;         // 배송지
    private String payment;         // 결제수단
}