package com.age.b2b.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class OrderDetailForShipmentDto {
    private Long orderId;
    private String orderNumber;
    private String clientName; // 업체명

    // 배송지 정보
    private String recipientName;
    private String recipientPhone;
    private String zipCode;
    private String address;
    private String detailAddress;
    private String memo; // 배송 메모

    // 주문 상품 목록
    private List<ShipmentItemDto> items;
}