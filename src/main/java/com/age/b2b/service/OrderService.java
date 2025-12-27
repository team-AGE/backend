package com.age.b2b.service;

import com.age.b2b.domain.*;
import com.age.b2b.domain.common.OrderStatus;
import com.age.b2b.dto.OrderDto;
import com.age.b2b.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    private final String TOSS_SECRET_KEY = "test_sk_XZYkKL4MrjBYmdBMpbB1r0zJwlEW";

    /**
     * [섹션 2] 파트너 대시보드 통계 조회
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDashboardStats(Client client) {
        return List.of(
                createStatMap("결제대기", orderRepository.countByClientAndStatus(client, OrderStatus.PENDING), "#f5f5f5"),
                createStatMap("배송대기", orderRepository.countByClientAndStatus(client, OrderStatus.PREPARING), "#237d31"),
                createStatMap("배송중", orderRepository.countByClientAndStatus(client, OrderStatus.SHIPPED), "#237d31"),
                createStatMap("배송완료", orderRepository.countByClientAndStatus(client, OrderStatus.DELIVERED), "#237d31")
        );
    }

    private Map<String, Object> createStatMap(String label, long count, String color) {
        Map<String, Object> map = new HashMap<>();
        map.put("label", label);
        map.put("count", count);
        map.put("color", color);
        return map;
    }

    /**
     * [본사 관리자용] 전체 발주 목록 조회 (AdminController 컴파일 에러 해결)
     */
    @Transactional(readOnly = true)
    public Page<OrderDto.AdminOrderListResponse> getAdminOrderList(
            Pageable pageable, String startDateStr, String endDateStr, String keyword) {

        LocalDateTime start = (startDateStr != null && !startDateStr.isEmpty())
                ? LocalDateTime.parse(startDateStr + "T00:00:00") : null;
        LocalDateTime end = (endDateStr != null && !endDateStr.isEmpty())
                ? LocalDateTime.parse(endDateStr + "T23:59:59") : null;

        Page<Order> orders = orderRepository.searchAdminOrders(start, end, keyword, pageable);

        return orders.map(order -> {
            OrderItem firstItem = order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0);
            String displayName = (firstItem != null) ? firstItem.getProduct().getName() : "상품 정보 없음";
            if (order.getOrderItems().size() > 1) {
                displayName += " 외 " + (order.getOrderItems().size() - 1) + "건";
            }

            return OrderDto.AdminOrderListResponse.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .clientName(order.getClient().getBusinessName())
                    .createdAt(order.getCreatedAt().toString().replace("T", " ").substring(0, 16))
                    .repProductCode(firstItem != null ? firstItem.getProduct().getProductCode() : "-")
                    .repProductName(displayName) // orderName 대신 사용
                    .itemCount(order.getOrderItems().size())
                    .totalAmount(order.getTotalAmount())
                    .status(convertStatusToKorean(order.getStatus()))
                    .build();
        });
    }

    /**
     * [파트너용] 주문 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<OrderDto.PartnerOrderListResponse> getPartnerOrderList(
            Client client, Pageable pageable, String startDateStr, String endDateStr, String keyword) {

        LocalDateTime start = (startDateStr != null && !startDateStr.isEmpty()) ? LocalDateTime.parse(startDateStr + "T00:00:00") : null;
        LocalDateTime end = (endDateStr != null && !endDateStr.isEmpty()) ? LocalDateTime.parse(endDateStr + "T23:59:59") : null;

        Page<Order> orders = orderRepository.searchClientOrders(client.getClientId(), start, end, keyword, pageable);

        return orders.map(order -> {
            OrderItem firstItem = order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0);
            String displayName = (firstItem != null) ? firstItem.getProduct().getName() : "상품 정보 없음";
            if (order.getOrderItems().size() > 1) {
                displayName += " 외 " + (order.getOrderItems().size() - 1) + "건";
            }

            return OrderDto.PartnerOrderListResponse.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .createdAt(order.getCreatedAt().toString().replace("T", " ").substring(0, 16))
                    .repProductName(displayName) // orderName 필드가 없으므로 기존 필드 활용
                    .itemCount(order.getOrderItems().size())
                    .totalAmount(order.getTotalAmount())
                    .status(convertStatusToKorean(order.getStatus()))
                    .build();
        });
    }

    // --- 주문 상세 및 결제 로직 ---

    @Transactional(readOnly = true)
    public OrderDto.OrderPageData getOrderPageData(Client client) {
        return OrderDto.OrderPageData.builder()
                .ownerName(client.getOwnerName())
                .phone(client.getPhone())
                .zipCode(client.getZipCode())
                .address(client.getAddress())
                .detailAddress(client.getDetailAddress())
                .build();
    }

    public OrderDto.OrderResponse createOrder(Client client, OrderDto.OrderRequest request) {
        Order order = new Order();
        order.setClient(client);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);

        order.setDeliveryInfo(new DeliveryInfo(
                request.getReceiverName(), request.getReceiverPhone(),
                request.getZipCode(), request.getAddress(),
                request.getDetailAddress(), request.getMemo()
        ));

        int totalAmount = 0;
        String firstProductName = "";
        int itemsCount = 0;

        if (request.getCartItemIds() != null && !request.getCartItemIds().isEmpty()) {
            List<CartItem> cartItems = cartItemRepository.findAllById(request.getCartItemIds());
            for (int i = 0; i < cartItems.size(); i++) {
                CartItem cartItem = cartItems.get(i);
                order.addOrderItem(createOrderItem(order, cartItem.getProduct(), cartItem.getCount()));
                totalAmount += (cartItem.getProduct().getSupplyPrice() * cartItem.getCount());
                if (i == 0) firstProductName = cartItem.getProduct().getName();
            }
            itemsCount = cartItems.size();
        } else if (request.getOrderItems() != null && !request.getOrderItems().isEmpty()) {
            for (int i = 0; i < request.getOrderItems().size(); i++) {
                OrderDto.OrderItemRequest itemReq = request.getOrderItems().get(i);
                Product product = productRepository.findById(itemReq.getProductId()).orElseThrow();
                order.addOrderItem(createOrderItem(order, product, itemReq.getCount()));
                totalAmount += (product.getSupplyPrice() * itemReq.getCount());
                if (i == 0) firstProductName = product.getName();
            }
            itemsCount = request.getOrderItems().size();
        }

        order.setTotalAmount(totalAmount);
        orderRepository.save(order);

        String orderName = firstProductName + (itemsCount > 1 ? " 외 " + (itemsCount - 1) + "건" : "");

        return OrderDto.OrderResponse.builder()
                .orderNumber(order.getOrderNumber()).orderName(orderName)
                .totalAmount(totalAmount).buyerName(client.getOwnerName())
                .buyerEmail(client.getEmail()).buyerTel(client.getPhone()).build();
    }

    private OrderItem createOrderItem(Order order, Product product, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setCount(count);
        orderItem.setPrice(product.getSupplyPrice());
        return orderItem;
    }

    public void verifyAndCompleteTossPayment(String paymentKey, String orderId, int amount) {
        Order order = orderRepository.findByOrderNumber(orderId).orElseThrow();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((TOSS_SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8)));
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.tosspayments.com/v1/payments/confirm",
                    new HttpEntity<>(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount), headers),
                    String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                order.setStatus(OrderStatus.PREPARING);
                deleteCartItemsAfterOrder(order);
            }
        } catch (Exception e) { throw new IllegalArgumentException("결제 승인 실패"); }
    }

    private void deleteCartItemsAfterOrder(Order order) {
        Cart cart = cartRepository.findByClient(order.getClient()).orElse(null);
        if (cart == null) return;
        List<Long> productIds = order.getOrderItems().stream().map(oi -> oi.getProduct().getId()).toList();
        cartItemRepository.deleteAll(cart.getCartItems().stream().filter(ci -> productIds.contains(ci.getProduct().getId())).toList());
    }

    private String generateOrderNumber() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    @Transactional(readOnly = true)
    public List<OrderDto.OrderItemDetail> getOrderItems(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        return order.getOrderItems().stream().map(item -> OrderDto.OrderItemDetail.builder()
                .productCode(item.getProduct().getProductCode()).productName(item.getProduct().getName())
                .price(item.getPrice()).count(item.getCount()).totalPrice(item.getPrice() * item.getCount()).build()).toList();
    }

    private String convertStatusToKorean(OrderStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING -> "결제대기";
            case PREPARING -> "상품준비중";
            case SHIPPED -> "배송중";
            case DELIVERED -> "배송완료";
            case CANCEL_REQUESTED -> "취소요청";
            case CANCELLED -> "취소완료";
            case RETURN_REQUESTED -> "반품요청";
            case RETURNED -> "반품완료";
            default -> status.name();
        };
    }

    // --- 취소/반품/관리자 관리 로직 ---

    public void cancelOrdersByAdmin(List<Long> orderIds) {
        List<Order> orders = orderRepository.findAllById(orderIds);
        for (Order order : orders) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCanceledAt(LocalDateTime.now());
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderDto.AdminReturnListResponse> getAdminReturnList(Pageable pageable, String keyword) {
        return orderRepository.findByStatusAndKeyword(OrderStatus.RETURN_REQUESTED, keyword, pageable)
                .map(order -> OrderDto.AdminReturnListResponse.builder()
                        .orderId(order.getId()).orderNumber(order.getOrderNumber())
                        .status(convertStatusToKorean(order.getStatus())).build());
    }

    public void approveReturns(List<Long> orderIds) {
        orderRepository.findAllById(orderIds).forEach(o -> o.setStatus(OrderStatus.RETURNED));
    }

    public void rejectReturn(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.RETURN_REJECTED);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto.AdminCancelListResponse> getAdminCancelList(Pageable pageable, String keyword) {
        return orderRepository.findByStatusAndKeyword(OrderStatus.CANCEL_REQUESTED, keyword, pageable)
                .map(order -> OrderDto.AdminCancelListResponse.builder()
                        .orderId(order.getId()).orderNumber(order.getOrderNumber())
                        .status(convertStatusToKorean(order.getStatus())).build());
    }

    public void rejectCancel(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CANCEL_REJECTED);
    }
    // [파트너] 취소 신청 (PartnerController 에러 해결)
    public void requestCancel(Client client, OrderDto.CancelRequest request) {
        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            throw new IllegalArgumentException("취소할 주문이 선택되지 않았습니다.");
        }

        List<Order> orders = orderRepository.findAllById(request.getOrderIds());

        for (Order order : orders) {
            // 본인 확인
            if (!order.getClient().getClientId().equals(client.getClientId())) {
                throw new IllegalArgumentException("본인의 주문만 취소 신청할 수 있습니다.");
            }

            if (order.getStatus() == OrderStatus.PREPARING) {
                order.setStatus(OrderStatus.CANCEL_REQUESTED);
                order.setCancelReason(request.getCancelReason()); // DTO에서 사유 추출
                order.setUpdatedAt(LocalDateTime.now());
            } else if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCanceledAt(LocalDateTime.now());
            } else {
                throw new IllegalStateException("취소 가능한 상태가 아닙니다.");
            }
        }
    }

    // [파트너] 반품 신청
    public void requestReturn(Client client, OrderDto.ReturnRequest request) {
        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            throw new IllegalArgumentException("반품할 주문이 선택되지 않았습니다.");
        }

        List<Order> orders = orderRepository.findAllById(request.getOrderIds());

        for (Order order : orders) {
            if (!order.getClient().getClientId().equals(client.getClientId())) {
                throw new IllegalArgumentException("본인의 주문만 반품 신청할 수 있습니다.");
            }

            if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
                order.setStatus(OrderStatus.RETURN_REQUESTED);
                order.setReturnReason(request.getReturnReason()); // DTO에서 사유 추출
                order.setUpdatedAt(LocalDateTime.now());
            } else {
                throw new IllegalStateException("배송 중 또는 배송 완료 상태에서만 반품이 가능합니다.");
            }
        }
    }
}