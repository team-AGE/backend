package com.age.b2b.domain.common;

public enum OrderStatus {
    ORDERED, // 발주(출고 전)
    SHIPPED, // 출고완료
    IN_DELIVERY, // 배송중
    DELIVERED, // 배송완료
    CANCELLED, // 취소됨
}
