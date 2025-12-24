package com.age.b2b.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderStatusCountRes {
    private long requestCnt;   // 발주
    private long outCnt;       // 출고요청(또는 출고)
    private long deliveryCnt;  // 배송중
    private long completeCnt;  // 배송완료
}
