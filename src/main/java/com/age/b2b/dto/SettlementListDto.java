package com.age.b2b.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class SettlementListDto {

    private String orderNumber;       // 발주번호
    private LocalDateTime orderDate;  // 발주일자
    private Long totalAmount;         // 발주총금액
    private String payMethod;         // 결제수단
    private String payStatus;         // 정산상태
    private LocalDateTime payDate;    // 정산(결제)일자

    // 🔥 JPQL new 연산자용 생성자 (필수)
    public SettlementListDto(
            String orderNumber,
            LocalDateTime orderDate,
            Long totalAmount,
            String payMethod,
            String payStatus,
            LocalDateTime payDate
    ) {
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.payMethod = payMethod;
        this.payStatus = payStatus;
        this.payDate = payDate;
    }
}
