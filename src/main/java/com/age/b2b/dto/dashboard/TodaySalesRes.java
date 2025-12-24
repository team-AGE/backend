package com.age.b2b.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TodaySalesRes {
    private long todaySales;
    private long yesterdaySales;
    private double rate; // 전일 대비 %
}
