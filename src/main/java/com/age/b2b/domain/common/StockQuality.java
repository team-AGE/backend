package com.age.b2b.domain.common;

// 재고 상태 (유통기한 기준)
public enum StockQuality {
    NORMAL,     // 정상재고 (1년 이상)
    MANAGED,    // 관리재고 (1년 미만)
    CAUTION,    // 주의재고 (90일 미만)
    DISPOSAL    // 폐기대상
}
