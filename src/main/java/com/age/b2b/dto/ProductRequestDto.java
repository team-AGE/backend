package com.age.b2b.dto;

import com.age.b2b.domain.common.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDto {
    private String productCode;   // 상품코드
    private String name;          // 상품명
    private int consumerPrice;    // 소비자가
    private int supplyPrice;      // 공급가
    private int costPrice;        // 제조원가
    private String origin;        // 원산지
    private String description;   // 상품설명
    private ProductStatus status; // 상품상태 (ON_SALE, TEMPORARY_OUT, DISCONTINUED)
}