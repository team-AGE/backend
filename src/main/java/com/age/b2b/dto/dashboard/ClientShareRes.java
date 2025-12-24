package com.age.b2b.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClientShareRes {
    private String clientName;
    private long sales;
    private double share; // 점유율(%) - 서비스에서 계산
}
