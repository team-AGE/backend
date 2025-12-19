package com.age.b2b.dto;

import com.age.b2b.domain.ProductLot;
import com.age.b2b.domain.common.StockQuality;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingResponseDto {

    private Long id;              // PK (수정/삭제용)
    private String orderNo;       // 발주번호 (여기선 Lot 번호로 대체)
    private String productCode;   // 상품코드
    private String productName;   // 상품명
    private int supplyPrice;      // 공급가
    private int qty;              // 수량
    private long totalPrice;      // 총 금액 (공급가 * 수량)
    private LocalDate expireDate; // 유통기한
    private String stockStatus;   // 재고상태 (정상, 불량 등)
    private String origin;        // 원산지

    // 엔티티 -> DTO 변환
    public static ReceivingResponseDto from(ProductLot lot) {
        return ReceivingResponseDto.builder()
                .id(lot.getId())
                .orderNo(lot.getLotNumber()) // Lot 번호를 발주번호처럼 사용
                .productCode(lot.getProduct().getProductCode())
                .productName(lot.getProduct().getName())
                .supplyPrice(lot.getProduct().getSupplyPrice())
                .qty(lot.getQuantity())
                // 총 금액 자동 계산
                .totalPrice((long) lot.getProduct().getSupplyPrice() * lot.getQuantity())
                .expireDate(lot.getExpiryDate())
                .stockStatus(convertQuality(lot.getStockQuality()))
                .origin(lot.getProduct().getOrigin())
                .build();
    }

    // Enum -> 한글 변환 헬퍼
    private static String convertQuality(StockQuality quality) {
        if (quality == null) return "알수없음";

        return switch (quality) {
            case NORMAL -> "정상재고";
            case MANAGED -> "관리재고";
            case CAUTION -> "주의재고";
            case DISPOSAL -> "폐기대상";
        };
    }
}