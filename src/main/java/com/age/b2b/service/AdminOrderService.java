package com.age.b2b.service;

import com.age.b2b.domain.Order;
import com.age.b2b.domain.common.OrderStatus;
import com.age.b2b.dto.AdminOrderUpdateDto;
import com.age.b2b.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;

    /**
     * [본사] 전체 발주 목록 조회 (2-1 메뉴)
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * [본사] 발주 상태 변경 (출고처리, 배송시작 등)
     */
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 상태 변경 로직 (실무에선 상태 전이 유효성 검사 필요: ex. 배송완료 -> 출고전 불가)
        order.setStatus(newStatus);

        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveryCompletedAt(LocalDateTime.now());
        }
    }

    /**
     * [본사] 취소 목록 조회 (2-3 메뉴)
     */
    @Transactional(readOnly = true)
    public List<Order> getCancelRequestedOrders() {
        // 반품 요청 상태인 주문만 조회
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.CANCEL_REQUESTED);
    }

    /**
     * [본사] 취소 요청 승인 처리 (2-3 메뉴)
     */
    public void approveCancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보가 없습니다."));

        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
            throw new IllegalStateException("취소 요청 상태인 주문만 승인 가능합니다.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCanceledAt(LocalDateTime.now()); // 취소일자 기록
    }

    /**
     * [본사] 반품 목록 조회 (2-2 메뉴)
     */
    @Transactional(readOnly = true)
    public List<Order> getReturnRequestedOrders() {
        // 반품 요청 상태인 주문만 조회
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.RETURN_REQUESTED);
    }

    /**
     * [본사] 반품 요청 승인 처리 (2-2 메뉴)
     */
    public void approveReturn(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보가 없습니다."));

        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new IllegalStateException("반품 요청 상태인 주문만 승인 가능합니다.");
        }

        order.setStatus(OrderStatus.RETURNED);
        order.setReturnedAt(LocalDateTime.now()); // 반품일자 기록
    }

    public void forceUpdateOrder(AdminOrderUpdateDto dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        // 관리자 권한으로 검증 없이 강제 수정
        if (dto.getStatus() != null) {
            order.setStatus(dto.getStatus());
            // 상태에 따라 날짜 자동 갱신 로직 추가 가능
        }

        if (dto.getTotalAmount() != null) {
            order.setTotalAmount(dto.getTotalAmount());
        }

        // 관리자 메모 기능이 있다면 order.setAdminMemo(dto.getMemo());
    }

    public void forceDeleteOrder(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new IllegalArgumentException("삭제할 주문이 없습니다.");
        }
        orderRepository.deleteById(orderId);
    }
}