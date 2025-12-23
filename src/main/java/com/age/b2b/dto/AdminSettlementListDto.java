package com.age.b2b.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class AdminSettlementListDto {
    private String businessName;    // clientName 대신 엔티티 필드명인 businessName으로 변경
    private String settlementNo;
    private String orderNumber;
    private LocalDateTime orderDate;
    private String settlementMonth;
    private LocalDateTime payDate;
    private String status;
    private String productCode;
    private String productName;
    private Integer supplyPrice;    // Integer 유지
    private Integer quantity;       // Integer 유지
    private Long totalAmount;

    public AdminSettlementListDto(String businessName, String settlementNo, String orderNumber,
                                  LocalDateTime orderDate, String settlementMonth, LocalDateTime payDate,
                                  String status, String productCode, String productName,
                                  Integer supplyPrice, Integer quantity, Long totalAmount) {
        this.businessName = businessName;
        this.settlementNo = settlementNo;
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.settlementMonth = settlementMonth;
        this.payDate = payDate;
        this.status = status;
        this.productCode = productCode;
        this.productName = productName;
        this.supplyPrice = supplyPrice;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }
}