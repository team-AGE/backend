package com.age.b2b.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingRequestDto {
    private String productCode;   // 상품코드
    private int qty;              // 수량
    private LocalDate expireDate; // 유통기한
    private String origin;        // 원산지 (입고 시 변경될 수 있으므로 받음)
}