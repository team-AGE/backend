package com.age.b2b.domain.common;

// 상품 판매 상태
public enum ProductStatus {
    ON_SALE("발주가능"),
    TEMPORARY_OUT("일시품절"),
    DISCONTINUED("품절(단종)");

    private final String description;

    ProductStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}