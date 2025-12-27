package com.age.b2b.repository;

import com.age.b2b.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<Order, Long> {

    // ===== 1) 오늘 매출 (o.created_at 사용) =====
    @Query(value = """
SELECT COALESCE(SUM(s.total_amount), 0)
FROM settlements s
WHERE s.created_at >= DATE(CONVERT_TZ(NOW(), '+00:00', '+09:00'))
  AND s.created_at <  DATE_ADD(DATE(CONVERT_TZ(NOW(), '+00:00', '+09:00')), INTERVAL 1 DAY)
""", nativeQuery = true)
    Long todaySettlementSales();


    // ===== 2) 전일 매출 (o.created_at 사용) =====
    @Query(value = """
SELECT COALESCE(SUM(s.total_amount), 0)
FROM settlements s
WHERE s.created_at >= DATE_SUB(DATE(CONVERT_TZ(NOW(), '+00:00', '+09:00')), INTERVAL 1 DAY)
  AND s.created_at <  DATE(CONVERT_TZ(NOW(), '+00:00', '+09:00'))
""", nativeQuery = true)
    Long yesterdaySettlementSales();


    // ===== 3) 이번달 매출(정산 기준) =====
    @Query(value = """
        SELECT COALESCE(SUM(s.total_amount), 0)
        FROM settlements s
        WHERE s.created_at >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
          AND s.created_at < DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH)
        """, nativeQuery = true)
    Long monthSales();

    // ===== 4) 일별 매출 (o.created_at 사용) =====
    @Query(value = """
    SELECT 
        DATE_FORMAT(s.created_at, '%m/%d') AS date_label,
        COALESCE(SUM(s.total_amount), 0) AS sales
    FROM settlements s
    WHERE s.created_at >= DATE_SUB(CURDATE(), INTERVAL 4 DAY) -- 오늘 포함 최근 5일
    GROUP BY DATE_FORMAT(s.created_at, '%m/%d'), DATE(s.created_at)
    ORDER BY DATE(s.created_at) ASC
    """, nativeQuery = true)
    List<Object[]> dailySalesRaw();

    // ===== 5) 발주 현황 카드 =====
    @Query(value = """
        SELECT
            COALESCE(SUM(CASE WHEN o.status = 'PREPARING' THEN 1 ELSE 0 END), 0) AS request_cnt,
            COALESCE(SUM(CASE WHEN o.status = 'SHIPPED' THEN 1 ELSE 0 END), 0) AS out_cnt,
            COALESCE(SUM(CASE WHEN o.status = 'SHIPPED' THEN 1 ELSE 0 END), 0) AS delivery_cnt,
            COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END), 0) AS complete_cnt
        FROM orders o
        """, nativeQuery = true)
    Object[] orderStatusCountsRaw();

    // ===== 6) 고객사 매출 (o.created_at 사용) =====
    @Query(value = """
        SELECT
            c.business_name AS client_name,
            COALESCE(SUM(oi.count * oi.price), 0) AS sales
        FROM orders o
        JOIN order_items oi ON o.order_id = oi.order_id
        JOIN clients c ON o.client_id = c.client_id
        WHERE o.status = 'DELIVERED'
          AND o.created_at >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
          AND o.created_at < DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH)
        GROUP BY c.business_name
        ORDER BY sales DESC
        """, nativeQuery = true)
    List<Object[]> clientSalesRaw();

    // ===== 7) 발주내역   =====
    // DashboardRepository.java

    // DashboardRepository.java

    @Query(value = """
    SELECT c.business_name, COUNT(o.order_id) -- c.name 대신 c.business_name 사용
    FROM orders o
    JOIN clients c ON o.client_id = c.client_id
    WHERE o.status = 'PREPARING'
    GROUP BY c.business_name
    LIMIT 5
    """, nativeQuery = true)
    List<Object[]> findPreparingOrderSummary();
}