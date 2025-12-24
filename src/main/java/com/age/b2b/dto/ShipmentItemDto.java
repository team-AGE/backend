package com.age.b2b.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShipmentItemDto {
    private Long productId;
    private String productCode;
    private String productName;
    private int price;     // 공급가
    private int quantity;  // 주문 수량
    private int totalPrice;
}