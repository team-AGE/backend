package com.age.b2b.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReceivingUpdateDto {
    private Long productLotId;    // 수정할 LOT ID
    private int qty;              // 변경할 최종 수량
    private LocalDate expireDate; // 변경할 유통기한
    private String note;          // 수정 사유
}