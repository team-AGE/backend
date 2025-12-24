package com.age.b2b.domain.common;

// 주문/배송 상태
public enum OrderStatus {
    PENDING,          // 결제대기/발주확인전
    PREPARING,        // 출고전(상품준비중)
    SHIPPED,          // 배송중
    DELIVERED,        // 배송완료
    CANCEL_REQUESTED, // 취소요청
    CANCELLED,        // 취소완료
    RETURN_REQUESTED, // 반품요청
    RETURNED,         // 반품완료
    RETURN_REJECTED,
    CANCEL_REJECTED
}
