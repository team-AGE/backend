package com.age.b2b.service;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.Order;
import com.age.b2b.domain.Settlement;
import com.age.b2b.domain.common.ClientStatus;
import com.age.b2b.domain.common.OrderStatus;
import com.age.b2b.repository.ClientRepository;
import com.age.b2b.repository.OrderRepository;
import com.age.b2b.repository.SettlementRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Slf4j
@TestPropertySource(locations = "classpath:application-test.properties")
class SettlementServiceTest {

    @Autowired SettlementService settlementService;
    @Autowired SettlementRepository settlementRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired EntityManager em;

    // 테스트 헬퍼: 특정 날짜에 배송완료된 주문 생성
    private void createDeliveredOrder(Client client, int amount, LocalDateTime date) {
        Order order = new Order();
        order.setClient(client);
        order.setTotalAmount(amount);
        order.setStatus(OrderStatus.DELIVERED);
        order.setOrderNumber("ORD-" + System.nanoTime());

        // 1. 일단 저장 (이 시점에는 @PrePersist 때문에 '현재 시간'으로 저장됨)
        orderRepository.saveAndFlush(order);

        // 2. Native SQL을 사용해 DB 데이터를 강제로 '과거 날짜'로 수정 (JPA 무시)
        em.createNativeQuery("UPDATE orders SET created_at = :date WHERE order_id = :id")
                .setParameter("date", date)
                .setParameter("id", order.getId())
                .executeUpdate();

        // 3. 영속성 컨텍스트 초기화 (중요: 메모리에 남은 '현재 시간' 데이터 제거)
        em.clear();

        // (참고) em.clear()를 하면 client 객체도 영속성이 끊기므로,
        // 이후 로직에서 client를 쓸 때 주의해야 하지만,
        // 현재 테스트 구조상 settlementService가 새로 조회하므로 괜찮습니다.
    }

    @Test
    @DisplayName("월별 정산 생성 테스트")
    void createMonthlySettlementTest() {
        // given
        // 1. 고객사 생성
        Client client = new Client();
        client.setUsername("settle_client");
        client.setPassword("1234");
        client.setBusinessName("정산약국");
        client.setPhone("010-" + (int)(Math.random()*9000) + "-1234");
        client.setEmail("settle@test.com");
        client.setBusinessNumber("999-99-99999");
        client.setOwnerName("김정산");
        client.setAddress("부산");
        client.setClientCategory("PHARMACY");
        client.setApprovalStatus(ClientStatus.APPROVED);
        clientRepository.save(client);

        // 2. 12월 주문 생성 (정산 대상 O)
        createDeliveredOrder(client, 10000, LocalDateTime.of(2025, 12, 5, 10, 0));
        createDeliveredOrder(client, 20000, LocalDateTime.of(2025, 12, 20, 15, 0));

        // 3. 11월 주문 생성 (정산 대상 X - 제외되어야 함)
        createDeliveredOrder(client, 50000, LocalDateTime.of(2025, 11, 30, 23, 59));

        // 4. 배송중 주문 (정산 대상 X - 상태 불일치)
        Order shippingOrder = new Order();
        shippingOrder.setClient(client);
        shippingOrder.setTotalAmount(5000);
        shippingOrder.setStatus(OrderStatus.SHIPPED); // 배송중
        shippingOrder.setCreatedAt(LocalDateTime.of(2025, 12, 10, 10, 0));
        shippingOrder.setOrderNumber("ORD-SHIP-" + System.nanoTime());
        orderRepository.save(shippingOrder);

        em.flush();
        em.clear();

        // when
        // 2025년 12월 정산 실행
        settlementService.createMonthlySettlement(2025, 12);

        em.flush();
        em.clear();

        // then
        List<Settlement> results = settlementRepository.findBySettlementMonth("2025-12");

        assertEquals(1, results.size()); // 고객사가 1명이므로 정산 데이터 1개
        Settlement settlement = results.get(0);

        System.out.println("\n================ [정산 결과 확인] ================");
        log.info("정산월: {}", settlement.getSettlementMonth());
        log.info("고객사: {}", settlement.getClient().getBusinessName());
        log.info("총 정산금액: {}원 (기대값: 30000원)", settlement.getTotalAmount());
        System.out.println("==================================================\n");

        // 10000 + 20000 = 30000원이어야 함 (11월 주문 제외, 배송중 제외)
        assertEquals(30000, settlement.getTotalAmount());
    }
}