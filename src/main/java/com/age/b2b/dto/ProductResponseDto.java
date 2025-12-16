package com.age.b2b.dto;

import com.age.b2b.domain.Product;
import com.age.b2b.domain.common.ProductStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ProductResponseDto {
    // 엑셀 1-2 요구사항: 상품코드, 상품명, 공급가, 상품상태, 상품설명, 원산지
    private Long productId; // (수정/삭제용 ID)
    private String productCode;
    private String name;
    private int supplyPrice;
    private ProductStatus status;
    private String description;
    private String origin;

    private LocalDate expiryDate;

    // Entity -> DTO 변환 메서드 (편의성)
    public static ProductResponseDto from(Product product) {
        return ProductResponseDto.builder()
                .productId(product.getId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .supplyPrice(product.getSupplyPrice())
                .status(product.getStatus())
                .description(product.getDescription())
                .origin(product.getOrigin())
                .expiryDate(product.getExpiryDate())
                .build();
    }
}