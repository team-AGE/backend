// OrderSummaryRes.java
package com.age.b2b.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderSummaryRes {
    private String BusinessName; // 고객사 명칭
    private long count;        // 발주 건수
}