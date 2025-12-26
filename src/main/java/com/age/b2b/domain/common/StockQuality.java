package com.age.b2b.domain.common;

import lombok.Getter;

// 재고 상태 (유통기한 기준)
@Getter
public enum StockQuality {
    NORMAL("정상재고"),     // 1년 이상
    MANAGED("관리재고"),    // 1년 미만
    CAUTION("주의재고"),    // 90일 미만
    DISPOSAL("폐기대상");   // 폐기대상

    private final String description;

    StockQuality(String description) {
        this.description = description;
    }
}