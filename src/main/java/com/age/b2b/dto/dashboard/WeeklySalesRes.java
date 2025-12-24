package com.age.b2b.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeeklySalesRes {
    private String week;  // "1주차" 같은 표시용이 아니라, 주차키(예: "01" "02") 추천
    private long sales;
}
