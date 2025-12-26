package com.age.b2b.service;

import com.age.b2b.domain.Order;
import com.age.b2b.domain.OrderItem;
import com.age.b2b.domain.ProductLot;
import com.age.b2b.domain.Shipment;
import com.age.b2b.domain.common.OrderStatus;
import com.age.b2b.dto.OrderDetailForShipmentDto;
import com.age.b2b.dto.ShipmentCreateDto;
import com.age.b2b.dto.ShipmentItemDto;
import com.age.b2b.dto.ShipmentListResponseDto;
import com.age.b2b.repository.OrderRepository;
import com.age.b2b.repository.ProductLotRepository;
import com.age.b2b.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final InventoryService inventoryService;
    private final ProductLotRepository productLotRepository;

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

        // 재고 차감 로직 (FIFO)
        order.getOrderItems().forEach(item -> {
            inventoryService.deductStock(item.getProduct().getId(), item.getCount());
        });

        // 출고 생성
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setShipmentNumber("SHP-" + System.currentTimeMillis()); // 실무에선 별도 채번 로직 필요

        shipmentRepository.save(shipment);

        // 주문 상태 변경 (상품준비중 -> 출고완료)
        order.setStatus(OrderStatus.SHIPPED);

        return shipment.getId();
    }

    /**
     * [본사] 출고 목록 조회 (검색 + 페이징)
     */
    @Transactional(readOnly = true)
    public Page<ShipmentListResponseDto> getShipmentList(String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "shippedDate"));

        Page<Shipment> shipments = shipmentRepository.searchShipments(keyword, pageable);

        return shipments.map(this::convertToDto);
    }

    /**
     * [본사] 출고 삭제
     */
    public void deleteShipments(List<Long> shipmentIds) {
        for (Long id : shipmentIds) {
            Shipment shipment = shipmentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("출고 정보를 찾을 수 없습니다."));

            Order order = shipment.getOrder();

            // 1. 재고 복구
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.restoreStock(item.getProduct().getId(), item.getCount());
            }

            // 2. 주문 상태 되돌리기
            order.setStatus(OrderStatus.PREPARING);

            // 3. 출고 내역 삭제
            shipmentRepository.delete(shipment);
        }
    }

    // Entity -> DTO 변환 헬퍼
    private ShipmentListResponseDto convertToDto(Shipment shipment) {
        Order order = shipment.getOrder();
        OrderItem firstItem = order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0);

        String productCode = "-";
        String productName = "상품 없음";
        String origin = "-";
        String lotNumber = "-";
        String expiryDate = "-";
        int totalQty = 0;

        if (firstItem != null) {
            productCode = firstItem.getProduct().getProductCode();
            productName = firstItem.getProduct().getName();
            if (order.getOrderItems().size() > 1) {
                productName += " 외 " + (order.getOrderItems().size() - 1) + "건";
            }
            origin = firstItem.getProduct().getOrigin();
            totalQty = order.getOrderItems().stream().mapToInt(OrderItem::getCount).sum();

            // ★ [Lot 정보 조회 로직 추가]
            // 선입선출(FIFO)이므로, 해당 상품의 '유통기한이 가장 빠른' Lot 정보를 대표로 표시
            List<ProductLot> lots = productLotRepository.findByProductIdOrderByExpiryDateAsc(firstItem.getProduct().getId());
            if (!lots.isEmpty()) {
                // 가장 먼저 나갔을 것으로 추정되는 첫 번째 Lot 정보 사용
                ProductLot repLot = lots.get(0);
                lotNumber = repLot.getLotNumber();
                expiryDate = repLot.getExpiryDate().toString();
            }
        }

        return ShipmentListResponseDto.builder()
                .shipmentId(shipment.getId())
                .shipmentNumber(shipment.getShipmentNumber())
                .orderNumber(order.getOrderNumber())
                .orderDate(order.getCreatedAt().toLocalDate().toString())
                .productCode(productCode)
                .productName(productName)
                .quantity(totalQty)
                .lotNumber(lotNumber)
                .expiryDate(expiryDate)
                .stockStatus("출고완료")
                .origin(origin)
                .address(order.getDeliveryInfo().getAddress())
                .payment("카드결제")
                .build();
    }
}