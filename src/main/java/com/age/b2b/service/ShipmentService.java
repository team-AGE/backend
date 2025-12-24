package com.age.b2b.service;

import com.age.b2b.domain.Order;
import com.age.b2b.domain.Shipment;
import com.age.b2b.domain.common.OrderStatus;
import com.age.b2b.dto.OrderDetailForShipmentDto;
import com.age.b2b.dto.ShipmentCreateDto;
import com.age.b2b.dto.ShipmentItemDto;
import com.age.b2b.repository.OrderRepository;
import com.age.b2b.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;

    // 1. 화면 진입 시 데이터 조회
    @Transactional(readOnly = true)
    public OrderDetailForShipmentDto getOrderInfoForShipment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 없습니다."));

        // Entity -> DTO 변환
        List<ShipmentItemDto> items = order.getOrderItems().stream()
                .map(item -> ShipmentItemDto.builder()
                        .productId(item.getProduct().getId())
                        .productCode(item.getProduct().getProductCode())
                        .productName(item.getProduct().getName())
                        .price(item.getPrice())
                        .quantity(item.getCount())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderDetailForShipmentDto.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .clientName(order.getClient().getBusinessName())
                .recipientName(order.getDeliveryInfo().getReceiverName())
                .recipientPhone(order.getDeliveryInfo().getReceiverPhone())
                .zipCode(order.getDeliveryInfo().getZipCode())
                .address(order.getDeliveryInfo().getAddress())
                .detailAddress(order.getDeliveryInfo().getDetailAddress())
                .memo(order.getDeliveryInfo().getMemo())
                .items(items)
                .build();
    }

    // 2. 출고 등록 실행
    public Long createShipment(ShipmentCreateDto dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문이 없습니다."));

        // 이미 출고된 주문인지 확인 로직 추가 권장
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("이미 출고된 주문입니다.");
        }

        // 출고 생성
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setShipmentNumber("SHP-" + System.currentTimeMillis()); // 실무에선 별도 채번 로직 필요

        shipmentRepository.save(shipment);

        // 주문 상태 변경 (상품준비중 -> 출고완료)
        order.setStatus(OrderStatus.SHIPPED);

        return shipment.getId();
    }
}