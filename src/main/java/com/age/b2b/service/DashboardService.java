package com.age.b2b.service;

import com.age.b2b.dto.dashboard.*;
import com.age.b2b.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public DashboardRes getDashboard() {

        // 1) 오늘/어제 매출 (정산 테이블 'settlements' 기준)
        long today = safeLong(dashboardRepository.todaySettlementSales());
        long yesterday = safeLong(dashboardRepository.yesterdaySettlementSales());
        double rate = calcRate(today, yesterday);
        System.out.println("🔥 todaySettlementSales = " + today);
        System.out.println("🔥 yesterdaySettlementSales = " + yesterday);
        TodaySalesRes todayRes = new TodaySalesRes(today, yesterday, rate);

        // 2) 이번달 매출 (정산 테이블 기준)
        long monthSales = safeLong(dashboardRepository.monthSales());

        // 3) 일별 매출 현황 (최근 5일치, 데이터 없는 날은 0원 처리)
        List<Object[]> dailyRaw = dashboardRepository.dailySalesRaw();

        // 최근 5일치 날짜 맵 생성 (오늘 포함 역순 4일전까지)
        Map<String, Long> dailyMap = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");

        for (int i = 4; i >= 0; i--) {
            String dateKey = LocalDate.now().minusDays(i).format(formatter);
            dailyMap.put(dateKey, 0L);
        }

        // DB 데이터 매핑
        if (dailyRaw != null) {
            for (Object[] row : dailyRaw) {
                if (row == null || row.length < 2) continue;
                String label = row[0].toString(); // "12/23"
                if (dailyMap.containsKey(label)) {
                    dailyMap.put(label, toLong(row[1]));
                }
            }
        }

        List<WeeklySalesRes> dailySales = new ArrayList<>();
        dailyMap.forEach((date, sales) -> dailySales.add(new WeeklySalesRes(date, sales)));

        // 4) 상단 발주 현황 카운트 (발주, 출고, 배송중, 배송완료)
        Object rawStatus = dashboardRepository.orderStatusCountsRaw();
        long r1 = 0, r2 = 0, r3 = 0, r4 = 0;

        if (rawStatus != null) {
            Object[] row;
            if (rawStatus instanceof List<?> list && !list.isEmpty()) {
                row = (Object[]) list.get(0);
            } else if (rawStatus instanceof Object[] array) {
                row = (array.length > 0 && array[0] instanceof Object[] inner) ? inner : array;
            } else {
                row = new Object[0];
            }

            if (row.length >= 1) r1 = toLong(row[0]);
            if (row.length >= 2) r2 = toLong(row[1]);
            if (row.length >= 3) r3 = toLong(row[2]);
            if (row.length >= 4) r4 = toLong(row[3]);
        }
        OrderStatusCountRes orderStatus = new OrderStatusCountRes(r1, r2, r3, r4);

        // 5) 고객사 매출 점유율 (도넛 차트)
        List<Object[]> clientRaw = dashboardRepository.clientSalesRaw();
        long totalClientSales = 0;
        List<ClientShareRes> clientShare = new ArrayList<>();

        if (clientRaw != null) {
            for (Object[] row : clientRaw) {
                if (row != null && row.length > 1) totalClientSales += toLong(row[1]);
            }
            for (Object[] row : clientRaw) {
                if (row == null || row.length < 2) continue;
                String name = row[0] == null ? "-" : String.valueOf(row[0]);
                long sales = toLong(row[1]);
                double share = totalClientSales == 0 ? 0 : ((double) sales / totalClientSales) * 100.0;
                clientShare.add(new ClientShareRes(name, sales, round1(share)));
            }
        }

        // 6) 발주 내역 리스트 (이미지 하단 리스트 영역)
        List<Object[]> preparingRaw = dashboardRepository.findPreparingOrderSummary();
        List<OrderSummaryRes> orderSummaries = new ArrayList<>();

        if (preparingRaw != null) {
            for (Object[] row : preparingRaw) {
                if (row == null || row.length < 2) continue;
                String BusinessName = row[0] != null ? row[0].toString() : "알 수 없음";
                long count = toLong(row[1]);
                orderSummaries.add(new OrderSummaryRes(BusinessName, count));
            }
        }

        // 최종 DTO 조립 반환 (orderSummaries 필드가 DashboardRes에 추가되어 있어야 합니다)
        return new DashboardRes(todayRes, monthSales, dailySales, orderStatus, orderSummaries, clientShare);
    }

    /* 유틸리티 메서드 */
    private long safeLong(Long v) { return v == null ? 0L : v; }

    private double calcRate(long today, long yesterday) {
        if (yesterday <= 0) return 0.0;
        return round1(((double) (today - yesterday) / (double) yesterday) * 100.0);
    }

    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
        }
        return 0L;
    }
}