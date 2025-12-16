package com.age.b2b.dto;

import com.age.b2b.domain.ProductLot;
import com.age.b2b.domain.common.StockQuality;
import lombok.Builder;
import lombok.Getter;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Builder
public class InventoryResponseDto {
    // 1. 상품 정보
    private String productCode;
    private String productName;
    private String origin;
    private int costPrice;


    // 2. lot 정보
    private Long productLotId;
    private String lotNumber;
    private LocalDate inboundDate;
    private LocalDate expiredDate;
    private int quantity;         // 현재 수량
    private StockQuality status;  // 재고 상태

    // 3. 계산된 정보
    private long inventoryIdAssetValue;  // 재고자산액 (수량 * 원가)
    private long remainingDays;          // 잔여 재고일 (유통기간까지 남은 일수)

    // Entity -> DTO 변환 메서드
    public static InventoryResponseDto from(ProductLot lot) {
        long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), lot.getExpiryDate());
        long assetValue = (long) lot.getQuantity() * lot.getProduct().getCostPrice();

        return InventoryResponseDto.builder()
                .productLotId(lot.getId())
                .productCode(lot.getProduct().getProductCode())
                .productName(lot.getProduct().getName())
                .origin(lot.getProduct().getOrigin())
                .costPrice(lot.getProduct().getCostPrice())
                .lotNumber(lot.getLotNumber())
                .inboundDate(lot.getInboundDate())
                .expiredDate(lot.getExpiryDate())
                .quantity(lot.getQuantity())
                .status(lot.getStockQuality())
                .inventoryIdAssetValue(assetValue)
                .remainingDays(remainingDays)
                .build();
    }

}
