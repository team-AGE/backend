package com.age.b2b.domain.common;

// 재고 조정 사유
public enum AdjustmentReason {
    INBOUND,        // 입고
    OUTBOUND,       // 출고(판매)
    LOST,           // 분실
    DAMAGED,        // 파손
    EXPIRED,        // 유통기한 경과
    COUNT_MISMATCH, // 실사 차이
    ETC             // 기타
}
