package com.age.b2b.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class InboundRequestDto {
    private Long productId;       // 어떤 상품인지
    private String lotNumber;     // Lot 번호 (제조번호)
    private int quantity;         // 입고 수량
    private LocalDate expiryDate; // 유통기한
    private LocalDate inboundDate;// 입고일자
}