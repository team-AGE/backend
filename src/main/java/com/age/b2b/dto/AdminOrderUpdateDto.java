package com.age.b2b.dto;

import com.age.b2b.domain.common.OrderStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminOrderUpdateDto {
    private Long orderId;
    private OrderStatus status; // 강제 상태 변경
    private Integer totalAmount;    // 금액 강제 수정 (계산 오류 시 등)
    private String memo;        // 관리자 메모 (직권 수정 사유 등)
}
