package com.age.b2b.dto;

import com.age.b2b.domain.common.ProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDto {
    private String productCode;   // 상품코드
    @JsonProperty("prodName")
    private String name;          // 상품명
    @JsonProperty("prodMsrp")
    private int consumerPrice;    // 소비자가
    @JsonProperty("prodPrice")
    private int supplyPrice;      // 공급가
    @JsonProperty("prodCost")
    private int costPrice;       // 제조원가
    @JsonProperty("prodOrigin")
    private String origin;        // 원산지
    @JsonProperty("prodContent")
    private String description;   // 상품설명
    // 프론트: prodExpiry ("2024-12-31") -> 백엔드: LocalDate
    @JsonProperty("prodExpiry")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate expiryDate;

    // 프론트: prodStatus ("발주가능") -> 백엔드: ProductStatus Enum
    private ProductStatus status;

    // ★ 핵심: 프론트엔드에서 보내는 한글 상태값을 Enum으로 변환해서 받기
    @JsonProperty("prodStatus")
    public void setStatusFromKorean(String prodStatus) {
        if ("발주가능".equals(prodStatus)) {
            this.status = ProductStatus.ON_SALE;
        } else if ("일시품절".equals(prodStatus)) {
            this.status = ProductStatus.TEMPORARY_OUT;
        } else if ("품절(단종)".equals(prodStatus)) {
            this.status = ProductStatus.DISCONTINUED;
        } else {
            this.status = ProductStatus.ON_SALE; // 기본값
        }
    }
}