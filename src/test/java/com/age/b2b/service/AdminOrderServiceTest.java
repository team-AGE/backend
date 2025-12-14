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

    @Autowired AdminOrderService adminOrderService;
    @Autowired OrderRepository orderRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired ProductRepository productRepository;
    @Autowired EntityManager em;

    // 데이터 셋업 헬퍼
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
        createTestOrder("(주)가나약국", "오메가3");
        createTestOrder("(주)다라병원", "종합비타민");

        em.flush();
        em.clear();

        // when
        List<Order> orders = adminOrderService.getAllOrders();

        // then
        assertEquals(2, orders.size());

        System.out.println("\n================ [엑셀 메뉴 2-1: 발주 목록 조회] ================");
        for (Order order : orders) {
            String clientName = order.getClient().getBusinessName();
            for (OrderItem item : order.getOrderItems()) {
                log.info("발주번호: {}, 발주일자: {}, 고객사: {}, 상품명: {}, 수량: {}, 총금액: {}, 상태: {}",
                        order.getOrderNumber(),
                        order.getCreatedAt().toLocalDate(),
                        clientName,
                        item.getProduct().getName(),
                        item.getCount(),
                        order.getTotalAmount(),
                        order.getStatus()
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