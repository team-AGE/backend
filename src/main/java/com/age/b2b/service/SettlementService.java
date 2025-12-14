package com.age.b2b.service;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.Order;
import com.age.b2b.domain.Settlement;
import com.age.b2b.domain.common.OrderStatus;
import com.age.b2b.repository.ClientRepository;
import com.age.b2b.repository.OrderRepository;
import com.age.b2b.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;

    /**
     * [본사] 월별 정산 일괄 생성
     * @param year 정산 연도 (ex. 2025)
     * @param month 정산 월 (ex. 12)
     */
    public void createMonthlySettlement(int year, int month) {
        // 1. 날짜 범위 설정 (해당 월 1일 00:00:00 ~ 말일 23:59:59)
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDateTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDateTime = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        String settlementMonthStr = year + "-" + String.format("%02d", month); // "2025-12"

        // 2. 모든 고객사 조회
        List<Client> clients = clientRepository.findAll();

        for (Client client : clients) {
            // 2-1. 이미 정산되었는지 확인
            if (settlementRepository.existsByClientAndSettlementMonth(client, settlementMonthStr)) {
                log.info("이미 정산 완료된 건입니다. Client: {}, Month: {}", client.getBusinessName(), settlementMonthStr);
                continue;
            }

            // 2-2. 해당 월의 '배송완료' 주문 조회
            List<Order> orders = orderRepository.findByClientAndStatusAndCreatedAtBetween(
                    client, OrderStatus.DELIVERED, startDateTime, endDateTime
            );

            // 주문이 없으면 정산 데이터 생성 안 함 (0원 정산 필요 시 로직 변경 가능)
            if (orders.isEmpty()) {
                continue;
            }

            // 2-3. 금액 합산
            long totalAmount = orders.stream()
                    .mapToLong(Order::getTotalAmount)
                    .sum();

            // 2-4. 정산 엔티티 생성 및 저장
            Settlement settlement = new Settlement();
            settlement.setClient(client);
            settlement.setSettlementNumber("SET-" + settlementMonthStr + "-" + client.getClientId());
            settlement.setSettlementMonth(settlementMonthStr);
            settlement.setTotalAmount(totalAmount);
            settlement.setStatus("COMPLETED"); // 기본 완료 처리 (추후 '입금대기' 등으로 확장 가능)

            settlementRepository.save(settlement);
        }
    }
}
