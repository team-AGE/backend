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

    // 1. 주문 페이지 정보 조회
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

    // 2. 주문 생성 (결제 전 PENDING)
    public OrderDto.OrderResponse createOrder(Client client, OrderDto.OrderRequest request) {
        Order order = new Order();
        order.setClient(client);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);

        order.setDeliveryInfo(new DeliveryInfo(
                request.getReceiverName(), request.getReceiverPhone(),
                request.getAddress(), request.getMemo()
        ));

        int totalAmount = 0;
        String firstProductName = "";
        int itemsCount = 0;

        // [Case A] 장바구니를 통한 주문인 경우 (기존 로직)
        if (request.getCartItemIds() != null && !request.getCartItemIds().isEmpty()) {
            List<CartItem> cartItems = cartItemRepository.findAllById(request.getCartItemIds());
            if (cartItems.isEmpty()) {
                throw new IllegalArgumentException("주문할 장바구니 상품을 찾을 수 없습니다.");
            }

            for (int i = 0; i < cartItems.size(); i++) {
                CartItem cartItem = cartItems.get(i);
                Product product = cartItem.getProduct();

                OrderItem orderItem = createOrderItem(order, product, cartItem.getCount());
                order.addOrderItem(orderItem);

                totalAmount += (product.getSupplyPrice() * cartItem.getCount());
                if (i == 0) firstProductName = product.getName();
            }
            itemsCount = cartItems.size();
        }
        // [Case B] 상품 목록에서 바로 주문인 경우 (신규 로직)
        else if (request.getOrderItems() != null && !request.getOrderItems().isEmpty()) {
            List<OrderDto.OrderItemRequest> directItems = request.getOrderItems();

            for (int i = 0; i < directItems.size(); i++) {
                OrderDto.OrderItemRequest itemReq = directItems.get(i);

                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다. ID: " + itemReq.getProductId()));

                OrderItem orderItem = createOrderItem(order, product, itemReq.getCount());
                order.addOrderItem(orderItem);

                totalAmount += (product.getSupplyPrice() * itemReq.getCount());
                if (i == 0) firstProductName = product.getName();
            }
            itemsCount = directItems.size();
        }
        else {
            throw new IllegalArgumentException("주문할 상품이 없습니다.");
        }

        order.setTotalAmount(totalAmount);
        orderRepository.save(order);

        String orderName = firstProductName;
        if (itemsCount > 1) orderName += " 외 " + (itemsCount - 1) + "건";

        return OrderDto.OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .orderName(orderName)
                .totalAmount(totalAmount)
                .buyerName(client.getOwnerName())
                .buyerEmail(client.getEmail())
                .buyerTel(client.getPhone())
                .build();
    }

    // [헬퍼] OrderItem 생성 메서드 추출
    private OrderItem createOrderItem(Order order, Product product, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setCount(count);
        orderItem.setPrice(product.getSupplyPrice()); // 주문 시점 가격 고정
        return orderItem;
    }

    // 3. 토스페이먼츠 결제 승인
    public void verifyAndCompleteTossPayment(String paymentKey, String orderId, int amount) {
        Order order = orderRepository.findByOrderNumber(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        if (order.getTotalAmount() != amount) {
            throw new IllegalArgumentException("결제 금액 불일치");
        }

        // 토스 서버로 승인 요청
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        String encodedKey = Base64.getEncoder().encodeToString((TOSS_SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.tosspayments.com/v1/payments/confirm",
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                order.setStatus(OrderStatus.PREPARING);

                deleteCartItemsAfterOrder(order);
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("결제 승인 실패: " + e.getMessage());
        }
    }

    // 주문 완료된 상품을 장바구니에서 찾아서 삭제
    private void deleteCartItemsAfterOrder(Order order) {
        Client client = order.getClient();
        Cart cart = cartRepository.findByClient(client).orElse(null);
        if (cart == null) return;

        List<Long> orderedProductIds = order.getOrderItems().stream()
                .map(oi -> oi.getProduct().getId())
                .collect(Collectors.toList());

        List<CartItem> itemsToDelete = cart.getCartItems().stream()
                .filter(ci -> orderedProductIds.contains(ci.getProduct().getId()))
                .collect(Collectors.toList());

        cartItemRepository.deleteAll(itemsToDelete);
    }

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return date + "-" + random;
    }

    // [파트너] 주문 목록 조회
    @Transactional(readOnly = true)
    public Page<OrderDto.PartnerOrderListResponse> getPartnerOrderList(
            Client client, Pageable pageable, String startDateStr, String endDateStr, String keyword) {

        LocalDateTime start = (startDateStr != null && !startDateStr.isEmpty())
                ? LocalDateTime.parse(startDateStr + "T00:00:00") : null;
        LocalDateTime end = (endDateStr != null && !endDateStr.isEmpty())
                ? LocalDateTime.parse(endDateStr + "T23:59:59") : null;

        Page<Order> orders = orderRepository.searchClientOrders(
                client.getClientId(), start, end, keyword, pageable);

        return orders.map(order -> {
            OrderItem firstItem = order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0);
            int totalQty = order.getOrderItems().stream().mapToInt(OrderItem::getCount).sum();

            return OrderDto.PartnerOrderListResponse.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .createdAt(order.getCreatedAt().toString().replace("T", " ").substring(0, 16))
                    .repProductCode(firstItem != null ? firstItem.getProduct().getProductCode() : "-")
                    .repProductName(firstItem != null ? firstItem.getProduct().getName() : "상품 없음")
                    .itemCount(order.getOrderItems().size())
                    .repProductPrice(firstItem != null ? firstItem.getPrice() : 0)
                    .totalQuantity(totalQty)
                    .totalAmount(order.getTotalAmount())
                    .status(convertStatusToKorean(order.getStatus()))
                    .deliveryDate(order.getDeliveryCompletedAt() != null ?
                            order.getDeliveryCompletedAt().toString().substring(0, 10) : "-")
                    .build();
        });
    }

    // 주문 상세 품목 조회
    @Transactional(readOnly = true)
    public List<OrderDto.OrderItemDetail> getOrderItems(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

        return order.getOrderItems().stream()
                .map(item -> OrderDto.OrderItemDetail.builder()
                        .productCode(item.getProduct().getProductCode())
                        .productName(item.getProduct().getName())
                        .price(item.getPrice())
                        .count(item.getCount())
                        .totalPrice(item.getPrice() * item.getCount())
                        .build())
                .collect(Collectors.toList());
    }

    // 상태 한글 변환 헬퍼
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

    // 본사 관리자용 주문 목록 조회
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

            return OrderDto.AdminOrderListResponse.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .clientName(order.getClient().getBusinessName()) // 업체명
                    .createdAt(order.getCreatedAt().toString().replace("T", " ").substring(0, 16))
                    .repProductCode(firstItem != null ? firstItem.getProduct().getProductCode() : "-")
                    .repProductName(firstItem != null ? firstItem.getProduct().getName() : "상품 없음")
                    .itemCount(order.getOrderItems().size())
                    .totalAmount(order.getTotalAmount())
                    .status(convertStatusToKorean(order.getStatus()))
                    .build();
        });
    }

    // 본사용 주문 취소 (상태 변경)
    public void cancelOrdersByAdmin(List<Long> orderIds) {
        List<Order> orders = orderRepository.findAllById(orderIds);
        for (Order order : orders) {
            // 이미 배송중이거나 완료된 건은 취소 불가 체크 로직이 필요하다면 추가
            order.setStatus(OrderStatus.CANCELLED);
        }
    }

    // --- 파트너 취소 신청 로직 ---
    public void requestCancel(Client client, OrderDto.CancelRequest request) {
        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            throw new IllegalArgumentException("취소할 주문이 선택되지 않았습니다.");
        }

        List<Order> orders = orderRepository.findAllById(request.getOrderIds());

        for (Order order : orders) {
            // 1. 본인 주문인지 확인
            if (!order.getClient().getClientId().equals(client.getClientId())) {
                throw new IllegalArgumentException("본인의 주문만 취소 신청할 수 있습니다.");
            }

            // 2. 취소 가능한 상태인지 확인 ('상품준비중'일 때만 신청 가능하도록 설정)
            if (order.getStatus() == OrderStatus.PREPARING) {
                order.setStatus(OrderStatus.CANCEL_REQUESTED); // 상태 변경
                order.setCancelReason(request.getCancelReason()); // 사유 저장
                order.setCancelDetail(request.getCancelDetail()); // 상세 사유 저장
                order.setUpdatedAt(LocalDateTime.now());
            } else if (order.getStatus() == OrderStatus.PENDING) {
                // 결제 대기 중이면 바로 취소 처리
                order.setStatus(OrderStatus.CANCELLED);
                order.setCanceledAt(LocalDateTime.now());
            } else {
                // 이미 배송됨 등의 사유로 신청 불가 시 예외 발생 또는 무시
                throw new IllegalStateException("주문번호 " + order.getOrderNumber() + "은(는) 취소 신청 가능한 상태가 아닙니다.");
            }
        }
    }
}