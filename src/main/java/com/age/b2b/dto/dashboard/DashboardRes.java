package com.age.b2b.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardRes {
    private TodaySalesRes today;
    private long monthSales;
    private List<WeeklySalesRes> weeklySales;
    private OrderStatusCountRes orderStatus;
    private List<OrderSummaryRes> orderSummaries;
    private List<ClientShareRes> clientShare;
}
