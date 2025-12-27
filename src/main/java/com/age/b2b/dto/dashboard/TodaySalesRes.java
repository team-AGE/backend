package com.age.b2b.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TodaySalesRes {
    private long today;
    private long yesterday;
    private double rate; // 전일 대비 %
}
