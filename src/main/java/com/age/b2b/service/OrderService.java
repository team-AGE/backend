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
    private final InventoryService inventoryService;

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
                request.getReceiverName(),
                request.getReceiverPhone(),
                request.getZipCode(),
                request.getAddress(),
                request.getDetailAddress(),
                request.getMemo()
        ));

        int totalAmount = 0;
        String firstProductName = "";
        int itemsCount = 0;

        // [Case A] 장바구니 주문
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
        // [Case B] 바로 주문
        else if (request.getOrderItems() != null && !request.getOrderItems().isEmpty()) {
            List<OrderDto.OrderItemRequest> directItems = request.getOrderItems();

            for (int i = 0; i < directItems.size(); i++) {
                OrderDto.OrderItemRequest itemReq = directItems.get(i);
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("상품 ID: " + itemReq.getProductId()));

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
        //if (itemsCount > 1) orderName += " 외 " + (itemsCount - 1) + "건";

        return OrderDto.OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .orderName(orderName)
                .totalAmount(totalAmount)
                .buyerName(client.getOwnerName())
                .buyerEmail(client.getEmail())
                .buyerTel(client.getPhone())
                .build();
    }

    // [헬퍼] OrderItem 생성
    private OrderItem createOrderItem(Order order, Product product, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setCount(count);
        orderItem.setPrice(product.getSupplyPrice());
        return orderItem;
    }

    // 3. 토스페이먼츠 결제 승인
    public void verifyAndCompleteTossPayment(String paymentKey, String orderId, int amount) {
        Order order = orderRepository.findByOrderNumber(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        if (order.getTotalAmount() != amount) {
            throw new IllegalArgumentException("결제 금액 불일치");
        }

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

    // 장바구니 비우기
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

    /**
     * [파트너용] 주문 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<OrderDto.PartnerOrderListResponse> getPartnerOrderList(
            Client client, Pageable pageable, String startDateStr, String endDateStr, String keyword) {

        LocalDateTime start = (startDateStr != null && !startDateStr.isEmpty()) ? LocalDateTime.parse(startDateStr + "T00:00:00") : null;
        LocalDateTime end = (endDateStr != null && !endDateStr.isEmpty()) ? LocalDateTime.parse(endDateStr + "T23:59:59") : null;

        Page<Order> orders = orderRepository.searchClientOrders(client.getClientId(), start, end, keyword, pageable);

        // 날짜 포맷터
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return orders.map(order -> {
            OrderItem firstItem = order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0);

            // 대표 상품 정보 조회 (상품코드, 상품명, 가격)
            String displayName = "상품 정보 없음";
            String productCode = "-";
            int productPrice = 0;

            if (firstItem != null) {
                displayName = firstItem.getProduct().getName();
                productCode = firstItem.getProduct().getProductCode(); // ★ 상품코드 가져오기
                productPrice = firstItem.getProduct().getSupplyPrice(); // ★ 단가 가져오기
            }

            // "상품명 외 N건" 처리
           // if (order.getOrderItems().size() > 1) {
           //     displayName += " 외 " + (order.getOrderItems().size() - 1) + "건";
          //  }

            // 총 수량 계산 (모든 아이템의 count 합계)
            int totalQty = order.getOrderItems().stream()
                    .mapToInt(OrderItem::getCount)
                    .sum();

            // 배송완료일자 처리
            String deliveryDateStr = "-";
            if (order.getStatus() == OrderStatus.DELIVERED && order.getDeliveryCompletedAt() != null) {
                deliveryDateStr = order.getDeliveryCompletedAt().format(formatter);
            }

            return OrderDto.PartnerOrderListResponse.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .createdAt(order.getCreatedAt().toString().replace("T", " ").substring(0, 16))

                    .repProductCode(productCode)
                    .repProductName(displayName)
                    .repProductPrice(productPrice)
                    .totalQuantity(totalQty)
                    .deliveryDate(deliveryDateStr)

                    .itemCount(order.getOrderItems().size())
                    .totalAmount(order.getTotalAmount())
                    .status(convertStatusToKorean(order.getStatus()))
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

    // 상태 한글 변환
    private String convertStatusToKorean(OrderStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING -> "결제대기";
            case PREPARING -> "상품준비중";
            case SHIPPED -> "배송중"; // 출고완료
            case DELIVERED -> "배송완료";
            case CANCEL_REQUESTED -> "취소요청";
            case CANCELLED -> "취소완료";
            case RETURN_REQUESTED -> "반품요청";
            case RETURNED -> "반품완료";
            case RETURN_REJECTED -> "반품거절";
            case CANCEL_REJECTED -> "취소거절";
            default -> status.name();
        };
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
               // displayName += " 외 " + (order.getOrderItems().size() - 1) + "건";
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

    // [본사] 취소 승인 (기존 cancelOrdersByAdmin 대신 이것 사용 권장)
    public void approveCancel(List<Long> orderIds) {
        List<Order> orders = orderRepository.findAllById(orderIds);
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.CANCEL_REQUESTED) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCanceledAt(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());
            }
        }
    }

    // [기존 코드 유지: 강제 취소 등]
    public void cancelOrdersByAdmin(List<Long> orderIds) {
        approveCancel(orderIds); // 안전하게 위 메서드로 위임
    }

    // [파트너] 취소 신청
    public void requestCancel(Client client, OrderDto.CancelRequest request) {
        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            throw new IllegalArgumentException("취소할 주문이 선택되지 않았습니다.");
        }

        List<Order> orders = orderRepository.findAllById(request.getOrderIds());

        for (Order order : orders) {
            if (!order.getClient().getClientId().equals(client.getClientId())) {
                throw new IllegalArgumentException("본인의 주문만 취소 신청할 수 있습니다.");
            }

            if (order.getStatus() == OrderStatus.PREPARING) {
                order.setStatus(OrderStatus.CANCEL_REQUESTED);
                order.setCancelReason(request.getCancelReason());
                order.setCancelDetail(request.getCancelDetail());
                order.setUpdatedAt(LocalDateTime.now());
            } else if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCanceledAt(LocalDateTime.now());
            } else {
                throw new IllegalStateException("주문번호 " + order.getOrderNumber() + "은(는) 취소 신청 가능한 상태가 아닙니다.");
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

            OrderStatus status = order.getStatus();
            if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
                order.setStatus(OrderStatus.RETURN_REQUESTED);
                order.setReturnReason(request.getReturnReason());
                order.setReturnDetail(request.getReturnDetail());
                order.setUpdatedAt(LocalDateTime.now());
            } else if (status == OrderStatus.PENDING || status == OrderStatus.PREPARING) {
                throw new IllegalStateException("주문번호 " + order.getOrderNumber() + "은(는) 배송 전 상태이므로 '취소' 신청을 이용해주세요.");
            } else {
                throw new IllegalStateException("주문번호 " + order.getOrderNumber() + "은(는) 이미 처리 중이거나 반품 신청이 불가능한 상태입니다.");
            }
        }
    }

    // [본사] 반품 관리 목록
    @Transactional(readOnly = true)
    public Page<OrderDto.AdminReturnListResponse> getAdminReturnList(
            Pageable pageable, String keyword) {
        // 조회할 상태 목록 정의 (요청, 완료, 거절 모두 포함)
        List<OrderStatus> statuses = List.of(
                OrderStatus.RETURN_REQUESTED,
                OrderStatus.RETURNED,
                OrderStatus.RETURN_REJECTED
        );

        // 변경된 리포지토리 메서드 호출
        Page<Order> orders = orderRepository.findByStatusInAndKeyword(
                statuses,
                keyword,
                pageable
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return orders.map(order -> {
            OrderItem firstItem = order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0);
            String productName = "상품 없음";
            String productCode = "-";
            int itemPrice = 0;

            if (firstItem != null) {
                productCode = firstItem.getProduct().getProductCode();
                productName = firstItem.getProduct().getName();
                itemPrice = firstItem.getPrice();
                if (order.getOrderItems().size() > 1) {
                   // productName += " 외 " + (order.getOrderItems().size() - 1) + "건";
                }
            }
            int totalQty = order.getOrderItems().stream().mapToInt(OrderItem::getCount).sum();

            String reqDate = order.getUpdatedAt() != null ? order.getUpdatedAt().format(formatter) : "-";

            // 2. 상태 처리일: 반품완료(returnedAt) 또는 거절(updatedAt)
            String procDate = "-";
            if (order.getStatus() == OrderStatus.RETURNED && order.getReturnedAt() != null) {
                procDate = order.getReturnedAt().format(formatter);
            } else if (order.getStatus() == OrderStatus.RETURN_REJECTED) {
                procDate = order.getUpdatedAt().format(formatter);
            }

            return OrderDto.AdminReturnListResponse.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .orderDate(order.getCreatedAt().toLocalDate().toString())

                    .productCode(productCode)
                    .productName(productName)
                    .supplyPrice(itemPrice)
                    .quantity(totalQty)
                    .totalAmount(order.getTotalAmount())

                    .returnReason(order.getReturnReason())
                    .status(convertStatusToKorean(order.getStatus()))

                    .returnRequestDate(reqDate)
                    .statusDate(procDate)
                    .build();
        });
    }

    // [본사] 반품 승인
    public void approveReturns(List<Long> orderIds) {
        List<Order> orders = orderRepository.findAllById(orderIds);
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.RETURN_REQUESTED) {

                // 재고 복구
                order.getOrderItems().forEach(item -> {
                    inventoryService.restoreStock(item.getProduct().getId(), item.getCount());
                });

                order.setStatus(OrderStatus.RETURNED);
                order.setReturnedAt(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());
            }
        }
    }

    // [본사] 반품 거절
    public void rejectReturn(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new IllegalStateException("반품 요청 상태인 주문만 거절할 수 있습니다.");
        }

        order.setStatus(OrderStatus.RETURN_REJECTED);
        String originalDetail = order.getReturnDetail() != null ? order.getReturnDetail() : "";
        order.setReturnDetail(originalDetail + " [반품거절사유: " + reason + "]");
        order.setUpdatedAt(LocalDateTime.now());
    }

    // [본사] 취소 관리 목록 조회
    @Transactional(readOnly = true)
    public Page<OrderDto.AdminCancelListResponse> getAdminCancelList(
            Pageable pageable, String keyword) {

        // 상태 필터링 (취소요청, 취소완료, 취소거절)
        List<OrderStatus> statuses = List.of(
                OrderStatus.CANCEL_REQUESTED,
                OrderStatus.CANCELLED,
                OrderStatus.CANCEL_REJECTED
        );

        Page<Order> orders = orderRepository.findByStatusInAndKeyword(statuses, keyword, pageable);

        // 날짜 포맷터 (yyyy-MM-dd HH:mm)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return orders.map(order -> {
            OrderItem item = order.getOrderItems().get(0);

            // 상품명 처리 (외 N건)
            String displayName = item.getProduct().getName();
            if (order.getOrderItems().size() > 1) {
               // displayName += " 외 " + (order.getOrderItems().size() - 1) + "건";
            }

            // 총 수량 계산
            int totalQty = order.getOrderItems().stream().mapToInt(OrderItem::getCount).sum();

            // ★ 날짜 로직
            // 1. 취소 요청일: 상태가 변경된 마지막 시간(updatedAt)을 사용 (요청 시점이므로)
            String reqDate = order.getUpdatedAt() != null ? order.getUpdatedAt().format(formatter) : "-";

            // 2. 상태 처리일:
            // - 취소완료(승인) -> canceledAt
            // - 취소거절 -> updatedAt (거절 시점에 업데이트됨)
            // - 취소요청(대기) -> 아직 처리 안됨 ("-")
            String procDate = "-";
            if (order.getStatus() == OrderStatus.CANCELLED && order.getCanceledAt() != null) {
                procDate = order.getCanceledAt().format(formatter);
            } else if (order.getStatus() == OrderStatus.CANCEL_REJECTED) {
                // 거절인 경우, 요청일과 겹칠 수 있으나 로직상 마지막 수정일이 처리일이 됨
                procDate = order.getUpdatedAt().format(formatter);
            }

            return OrderDto.AdminCancelListResponse.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .orderDate(order.getCreatedAt().toLocalDate().toString()) // 발주일자는 날짜만

                    .productCode(item.getProduct().getProductCode())
                    .productName(displayName)
                    .supplyPrice(item.getPrice()) // 공급가
                    .quantity(totalQty)
                    .totalAmount(order.getTotalAmount())

                    .cancelReason(order.getCancelReason())
                    .status(convertStatusToKorean(order.getStatus()))

                    // ★ 시간 포함된 날짜 매핑
                    .cancelRequestDate(reqDate)
                    .statusProcessingDate(procDate)
                    .build();
        });
    }

    // [본사] 취소 거절
    public void rejectCancel(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
            throw new IllegalStateException("취소 요청 상태인 주문만 거절할 수 있습니다.");
        }

        order.setStatus(OrderStatus.CANCEL_REJECTED);
        String originalDetail = order.getCancelDetail() != null ? order.getCancelDetail() : "";
        order.setCancelDetail(originalDetail + " [취소거절사유: " + reason + "]");
        order.setUpdatedAt(LocalDateTime.now());
    }

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
}