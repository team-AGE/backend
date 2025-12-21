package com.age.b2b.service;

import com.age.b2b.domain.*;
import com.age.b2b.domain.common.OrderStatus;
import com.age.b2b.dto.OrderDto;
import com.age.b2b.repository.*;
import lombok.RequiredArgsConstructor;
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
        List<CartItem> cartItems = cartItemRepository.findAllById(request.getCartItemIds());
        if (cartItems.isEmpty()) throw new IllegalArgumentException("주문할 상품이 없습니다.");

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

        for (int i = 0; i < cartItems.size(); i++) {
            CartItem cartItem = cartItems.get(i);
            Product product = cartItem.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setCount(cartItem.getCount());
            orderItem.setPrice(product.getSupplyPrice());

            order.addOrderItem(orderItem);
            totalAmount += (product.getSupplyPrice() * cartItem.getCount());

            if (i == 0) firstProductName = product.getName();
        }
        order.setTotalAmount(totalAmount);
        orderRepository.save(order);

        String orderName = firstProductName;
        if (cartItems.size() > 1) orderName += " 외 " + (cartItems.size() - 1) + "건";

        return OrderDto.OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .orderName(orderName)
                .totalAmount(totalAmount)
                .buyerName(client.getOwnerName())
                .buyerEmail(client.getEmail())
                .buyerTel(client.getPhone())
                .build();
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
        // 고객의 장바구니 찾기
        Cart cart = cartRepository.findByClient(client).orElse(null);
        if (cart == null) return;

        // 주문한 상품 ID 목록
        List<Long> orderedProductIds = order.getOrderItems().stream()
                .map(oi -> oi.getProduct().getId())
                .collect(Collectors.toList());

        // 장바구니 아이템 중, 주문한 상품과 일치하는 것만 필터링
        List<CartItem> itemsToDelete = cart.getCartItems().stream()
                .filter(ci -> orderedProductIds.contains(ci.getProduct().getId()))
                .collect(Collectors.toList());

        // 삭제 수행
        cartItemRepository.deleteAll(itemsToDelete);
    }

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return date + "-" + random;
    }
}