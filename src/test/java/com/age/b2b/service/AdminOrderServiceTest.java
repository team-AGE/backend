package com.age.b2b.service;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.Order;
import com.age.b2b.domain.OrderItem;
import com.age.b2b.domain.Product;
import com.age.b2b.domain.common.ClientStatus;
import com.age.b2b.domain.common.OrderStatus;
import com.age.b2b.domain.common.ProductStatus;
import com.age.b2b.repository.ClientRepository;
import com.age.b2b.repository.OrderRepository;
import com.age.b2b.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Slf4j
@TestPropertySource(locations = "classpath:application-test.properties")
class AdminOrderServiceTest {

    // 객체 의존성 주입
    @Autowired AdminOrderService adminOrderService;
    @Autowired OrderRepository orderRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired ProductRepository productRepository;
    @Autowired EntityManager em;

    // 데이터 셋업 헬퍼

    // createTestOrder 메서드를 쓰지 않으면 모든 테스트 메서드 (getAllOrdersTest, approveCancelTest 등) 내부에
    // new Client(), new Product(), new Order() 등 수십 줄의 객체 생성 및 저장 로직을 반복해서 직접 작성해줘야 함
    private Order createTestOrder(String clientName, String prodName) {
        Client client = new Client();
        client.setUsername("user_" + clientName);
        client.setBusinessName(clientName);
        client.setPassword("1234");
        String randomPhone = "010-" + (int)(Math.random() * 9000 + 1000) + "-" + (int)(Math.random() * 9000 + 1000);
        client.setPhone(randomPhone);
        client.setEmail("test" + clientName + "@email.com"); // 필수값 채움
        client.setBusinessNumber("123-" + clientName); // 필수값 채움
        client.setOwnerName("홍길동");
        client.setAddress("서울");
        client.setClientCategory("ETC");
        client.setApprovalStatus(ClientStatus.APPROVED);
        clientRepository.save(client);

        Product product = new Product();
        product.setProductCode("CODE_" + prodName);
        product.setName(prodName);
        product.setSupplyPrice(10000);
        product.setConsumerPrice(15000);
        product.setCostPrice(5000); // 필수값
        product.setOrigin("Korea"); // 필수값
        product.setDescription("Desc"); // 필수값
        product.setStatus(ProductStatus.ON_SALE);
        productRepository.save(product);

        Order order = new Order();
        order.setClient(client);
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setStatus(OrderStatus.PENDING); // 발주(배송전)
        order.setTotalAmount(50000);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setCount(5);
        item.setPrice(10000);
        order.addOrderItem(item);

        return orderRepository.save(order);
    }

    @Test
    @DisplayName("2-1. 발주 목록 조회 (본사 관리자)")
    void getAllOrdersTest() {
        // given
        createTestOrder("(주)가나약국", "오메가3");     // 테스트용 데이터를 “직접 만들어서 DB에 저장
        createTestOrder("(주)다라병원", "종합비타민");

        em.flush();     // DB에 강제 반영
        em.clear();     // 캐시 제거

        // when
        List<Order> orders = adminOrderService.getAllOrders();      // 전체 발주 목록을 DB에서 조회

        // then
        assertEquals(2, orders.size());     // 발주 목록 조회 기능이 정상 동작하는지 검증, 실제로 반환된 주문 개수가 2개인지 확인

        System.out.println("\n================ [엑셀 메뉴 2-1: 발주 목록 조회] ================");
        for (Order order : orders) {
            String clientName = order.getClient().getBusinessName();
            for (OrderItem item : order.getOrderItems()) {
                log.info("상품명: {}, 상품코드: {}, 발주번호: {}, 발주일자: {}, 고객사: {}, 공급가: {}, 수량: {}, 총금액: {}, 발주상태: {}",
                        item.getProduct().getName(),
                        item.getProduct().getProductCode(),
                        order.getOrderNumber(),
                        order.getCreatedAt().toLocalDate(),
                        clientName,
                        item.getPrice(),
                        item.getCount(),
                        order.getTotalAmount(),
                        order.getStatus()
                );
            }
        }
        System.out.println("==============================================================\n");
    }

    @Test
    @DisplayName("2-2. 반품 관리 (조회)")
    void getReturnOrdersTest() {
        // 테스트용 데이터를 “직접 만들어서 DB에 저장
        // 반품 요청을 한 주문 데이터
        Order returnOrder = createTestOrder("반품약국", "오메가3");
        returnOrder.setStatus(OrderStatus.RETURN_REQUESTED);

        // 주문 상태를 반품 요청으로 변경할 때, returnedAt (반품 요청일/완료일) 필드는 보통 시스템에 의해 현재 시각으로 자동 기록
        // returnedAt 필드에 값을 설정하지 않으면 Order 엔티티를 처음 생성할 때 설정된 기본값(대부분 null)이 그대로 유지
        // order.getReturnedAt()이 null을 반환하는 상황에서, 그 뒤에 .toLocalDate() 메서드를 호출하게 되면 Java는 NullPointerException을 발생
        // null이 아닌 유효한 LocalDateTime 객체를 반환하기 위해 반품 시각과 사유 추가
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        returnOrder.setReturnedAt(now);
        returnOrder.setReturnReason("단순 변심으로 인한 반품");
        orderRepository.save(returnOrder);


        // 반품 요청을 하지않은 주문 데이터
        createTestOrder("정상약국", "종합비타민");

        em.flush();     // DB에 강제 반영
        em.clear();     // 캐시 제거

        // when
        List<Order> orders = adminOrderService.getReturnRequestedOrders();  // 반품 목록을 DB에서 조회

        assertEquals(1, orders.size());
        assertEquals(OrderStatus.RETURN_REQUESTED, orders.get(0).getStatus());  // 조회된 데이터가 ‘반품 요청’ 상태인지 검증

        System.out.println("\n================ [엑셀 메뉴 2-2: 반품 관리 조회] ================");
        for (Order order : orders) {
            String clientName = order.getClient().getBusinessName();
            for (OrderItem item : order.getOrderItems()) {
                log.info("상품명: {}, 상품코드: {}, 발주번호: {}, 반품일자: {}, 수량: {}, 총금액: {}, 반품사유: {}",
                        item.getProduct().getName(),
                        item.getProduct().getProductCode(),
                        order.getOrderNumber(),
                        order.getReturnedAt().toLocalDate(),
                        item.getCount(),
                        order.getTotalAmount(),
                        order.getReturnReason()
                );
            }
        }
        System.out.println("==============================================================\n");

    }

    @Test
    @DisplayName("2-2. 반품 관리 (반품 승인)")
    void getApproveReturnTest() {
        // 테스트용 데이터를 “직접 만들어서 DB에 저장
        // 반품 승인 요청을 한 주문 데이터
        Order order = createTestOrder("반품승인약국", "오메가3");
        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnReason("제품 불량");
        orderRepository.save(order);

        Long orderId = order.getId();

        em.flush();     // DB에 강제 저장
        em.clear();     // 캐시 제거

        adminOrderService.approveReturn(orderId);   // 반품 승인 처리 (approveReturn) 호출

        // DB에서 특정 orderId에 해당하는 주문을 조회, 결과는 Optional<Order> 형태로 반환
        // .orElseThrow() : 만약 해당 ID가 DB에 없으면 예외를 발생, 테스트 실패 및 예외 종류와 메시지 표시
        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();

        assertEquals(OrderStatus.RETURNED, updatedOrder.getStatus());   // 반품 승인 후 주문 상태가 실제로 RETURNED로 변경됐는지 확인

        assertNotNull(updatedOrder.getReturnedAt());    // 반품 완료일자가 실제로 기록됐는지 확인
        assertEquals("제품 불량", updatedOrder.getReturnReason());  // 반품 사유가 DB에 올바르게 저장됐는지 확인

        System.out.println("\n================ [반품 승인 결과] ================");
        log.info("발주번호: {}, 반품 상태: {}, 반품일자: {}, 반품사유: {}",
                updatedOrder.getOrderNumber(),
                updatedOrder.getStatus(),
                updatedOrder.getReturnedAt(),
                updatedOrder.getReturnReason()
        );
        for (OrderItem item : updatedOrder.getOrderItems()) {
            log.info("상품명: {}, 수량: {}, 공급가: {}",
                    item.getProduct().getName(),
                    item.getCount(),
                    item.getPrice()
            );
        }
        System.out.println("==================================================\n");


    }

    @Test
    @DisplayName("2-3. 취소 관리 (조회)")
    void getCancelOrdersTest() {
        // 테스트용 데이터를 “직접 만들어서 DB에 저장
        // 취소 요청을 한 주문 데이터
        Order cancelOrder = createTestOrder("취소약국", "오메가3");
        cancelOrder.setStatus(OrderStatus.CANCEL_REQUESTED);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        cancelOrder.setCanceledAt(now);
        cancelOrder.setCancelReason("고객 단순 변심");
        orderRepository.save(cancelOrder);

        // 취소 요청을 하지않은 주문 데이터
        createTestOrder("정상약국", "종합비타민");

        em.flush();     // DB에 강제 저장
        em.clear();     // 캐시 제거

        List<Order> orders = adminOrderService.getCancelRequestedOrders();  // 취소 목록을 DB에서 조회

        assertEquals(1, orders.size());
        assertEquals(OrderStatus.CANCEL_REQUESTED, orders.get(0).getStatus());  // 조회된 데이터가 ‘취소 요청’ 상태인지 검증

        System.out.println("\n================ [엑셀 메뉴 2-3: 취소 관리 조회] ================");
        for (Order order : orders) {
            String clientName = order.getClient().getBusinessName();
            for (OrderItem item : order.getOrderItems()) {
                log.info("상품명: {}, 상품코드: {}, 취소일자: {}, 수량: {}, 총금액: {}, 취소사유: {}",
                        item.getProduct().getName(),
                        item.getProduct().getProductCode(),
                        order.getCanceledAt().toLocalDate(),
                        item.getCount(),
                        order.getTotalAmount(),
                        order.getCancelReason()
                );
            }
        }
        System.out.println("==============================================================\n");

    }

    @Test
    @DisplayName("2-3. 취소 관리 (취소 승인 테스트)")
    void approveCancelTest() {
        // given
        Order order = createTestOrder("취소요청약국", "반품상품");
        order.setStatus(OrderStatus.CANCEL_REQUESTED); // 고객이 취소 요청함
        orderRepository.save(order);

        Long orderId = order.getId();

        em.flush();
        em.clear();

        // when
        adminOrderService.approveCancel(orderId);

        // then
        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, updatedOrder.getStatus());

        System.out.println("\n================ [엑셀 메뉴 2-3: 취소 승인 결과] ================");
        log.info("발주번호: {}, 변경된 상태: {}", updatedOrder.getOrderNumber(), updatedOrder.getStatus());
        System.out.println("==============================================================\n");
    }
}