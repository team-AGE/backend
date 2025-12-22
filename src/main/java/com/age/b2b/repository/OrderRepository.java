package com.age.b2b.repository;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.Order;
import com.age.b2b.domain.common.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // [고객사] 내 주문 조회
    List<Order> findByClient_ClientIdOrderByCreatedAtDesc(Long clientId);

    // 주문번호로 조회 (결제 검증 시 사용)
    Optional<Order> findByOrderNumber(String orderNumber);

    // [본사] 전체 주문 목록 조회 (최신순)
    List<Order> findAllByOrderByCreatedAtDesc();

    // [본사] 상태별 주문 조회 (예: 취소요청 건만 보기)
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<Order> findByClientAndStatusAndCreatedAtBetween(
            Client client,
            OrderStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("SELECT o FROM Order o " +
            "WHERE o.client.clientId = :clientId " +
            "AND o.status <> 'PENDING' " +
            "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
            "AND (:keyword IS NULL OR o.orderNumber LIKE %:keyword%)")
    Page<Order> searchClientOrders(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}