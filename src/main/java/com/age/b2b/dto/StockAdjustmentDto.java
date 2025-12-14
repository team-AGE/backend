package com.age.b2b.dto;

import com.age.b2b.domain.common.AdjustmentReason;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockAdjustmentDto {
    private Long productLotId;      // 어떤 Lot를 조정할지
    private int changeQuantity;     // 변동 수량 (폐기는 음수, 실사 파악 후 추가는 양수)
    private AdjustmentReason reason;// 사유 (분실, 파손, 기타 등)
    private String note;            // 상세 비고
}