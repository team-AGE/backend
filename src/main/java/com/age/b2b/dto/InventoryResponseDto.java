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
    private String productCode;
    private String productName;
    private String origin;
    private int costPrice;

    private Long productLotId;
    private String lotNumber;
    private LocalDate inboundDate;
    private LocalDate expiredDate;
    private int quantity;
    private StockQuality status;

    private long inventoryIdAssetValue;
    private long remainingDays;

    // ★ [수정됨] 에러 방지용 안전한 변환 메서드
    public static InventoryResponseDto from(ProductLot lot) {

        // 1. 날짜 계산 (Null이면 0일 처리)
        long remainingDays = 0;
        if (lot.getExpiryDate() != null) {
            remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), lot.getExpiryDate());
        }

        // 2. 상품 정보 가져오기 (연결된 상품이 없어도 에러 안 나게 처리)
        String pCode = "UNKNOWN";
        String pName = "상품정보없음";
        String pOrigin = "-";
        int cost = 0;

        // product가 null이 아닐 때만 가져옴
        if (lot.getProduct() != null) {
            pCode = lot.getProduct().getProductCode();
            pName = lot.getProduct().getName();
            pOrigin = lot.getProduct().getOrigin();
            cost = lot.getProduct().getCostPrice();
        } else {
            // 비상용: Lot에 저장된 코드라도 사용
            if (lot.getProductCode() != null) pCode = lot.getProductCode();
        }

        // 3. 자산액 계산
        long assetValue = (long) lot.getQuantity() * cost;

        return InventoryResponseDto.builder()
                .productLotId(lot.getId())
                .productCode(pCode)
                .productName(pName)
                .origin(pOrigin)
                .costPrice(cost)
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