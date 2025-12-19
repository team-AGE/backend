package com.age.b2b.service;

import com.age.b2b.domain.InventoryLog;
import com.age.b2b.domain.Product;
import com.age.b2b.domain.ProductLot;
import com.age.b2b.domain.common.AdjustmentReason;
import com.age.b2b.domain.common.ProductStatus;
import com.age.b2b.dto.InboundRequestDto;
import com.age.b2b.dto.StockAdjustmentDto;
import com.age.b2b.repository.InventoryLogRepository;
import com.age.b2b.repository.ProductLotRepository;
import com.age.b2b.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Slf4j
@TestPropertySource(locations = "classpath:application-test.properties")
class InventoryServiceTest {

    @Autowired InventoryService inventoryService;
    @Autowired ProductRepository productRepository;
    @Autowired ProductLotRepository productLotRepository;
    @Autowired InventoryLogRepository inventoryLogRepository;
    @Autowired EntityManager em;

    // 테스트용 상품 생성 헬퍼
    private Product createProduct(String name, String code) {
        Product product = new Product();
        product.setName(name);
        product.setProductCode(code);
        product.setConsumerPrice(12000); // 소비자가
        product.setSupplyPrice(8000);    // 공급가
        product.setCostPrice(5000);      // 원가
        product.setStatus(ProductStatus.ON_SALE);
        product.setDescription("피로회복에 좋은 고함량 비타민");
        product.setOrigin("대한민국");
        return productRepository.save(product);
    }

    @Test
    @DisplayName("상품 입고 및 상품정보 조회 테스트")
    void inboundTest() {
        // given
        Product product = createProduct("프리미엄 비타민C", "VITA-001");

        InboundRequestDto req = InboundRequestDto.builder()
                .productId(product.getId())
                .lotNumber("LOT-20251214")
                .quantity(100)
                .inboundDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .build();

        // when
        Long lotId = inventoryService.registerInbound(req);

        em.flush();
        em.clear();

        // then
        // JPA가 DB에서 데이터를 가져와 new ProductLot()을 수행하여 결과 반환
        ProductLot lot = productLotRepository.findById(lotId).orElseThrow();
        // lot이 들고 있던 상품 객체의 주소(참조)를 findProduct라는 변수도 같이 가리키게 함
        // Lot과 연결된 상품 조회
        Product findProduct = lot.getProduct();

        System.out.println("\n================ [상품/입고 상세 조회] ================");
        // 요청하신 포맷대로 로그 출력
        log.info("상품조회: 상품코드: {}, 상품명: {}, 공급가: {}, 소비자가: {}, 상품상태: {}, 상품설명: {}, 원산지: {}",
                findProduct.getProductCode(),
                findProduct.getName(),
                findProduct.getSupplyPrice(),
                findProduct.getConsumerPrice(),
                findProduct.getStatus(),
                findProduct.getDescription(),
                findProduct.getOrigin()
        );
        log.info("입고내역: Lot번호: {}, 현재고: {}, 유통기한: {}, 재고상태: {}",
                lot.getLotNumber(),
                lot.getQuantity(),
                lot.getExpiryDate(),
                lot.getStockQuality()
        );
        System.out.println("===================================================================\n");

        assertEquals(100, lot.getQuantity());
    }

    @Test
    @DisplayName("재고 조정(폐기/파손) 및 로그 확인 테스트")
    void adjustmentTest() {
        // given (상품 입고)
        Product product = createProduct("홍삼 스틱", "RED-GINSENG-01");
        InboundRequestDto inboundReq = InboundRequestDto.builder()
                .productId(product.getId())
                .lotNumber("LOT-BROKEN-TEST")
                .quantity(50)
                .inboundDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .build();
        Long lotId = inventoryService.registerInbound(inboundReq);

        em.flush();
        em.clear();

        // when (파손 처리)
        StockAdjustmentDto adjReq = StockAdjustmentDto.builder()
                .productLotId(lotId)
                .changeQuantity(-5) // 5개 파손
                .reason(AdjustmentReason.DAMAGED)
                .note("창고 적재 중 박스 파손")
                .build();

        inventoryService.adjustStock(adjReq);

        em.flush();
        em.clear();

        // then
        List<InventoryLog> logs = inventoryLogRepository.findAll();
        // 마지막 로그(파손 처리) 가져오기
        InventoryLog lastLog = logs.get(logs.size() - 1);
        ProductLot lot = productLotRepository.findById(lotId).orElseThrow();

        System.out.println("\n================ [엑셀 메뉴트리: 재고 조정 이력 확인] ================");
        log.info("조정대상: 상품명: {}, Lot번호: {}", lot.getProduct().getName(), lot.getLotNumber());
        log.info("조정내역: 변동수량: {}, 변동사유: {}, 상세비고: {}, 변동 후 재고: {}",
                lastLog.getChangeQuantity(),
                lastLog.getReason(),
                lastLog.getNote(),
                lastLog.getCurrentQuantity()
        );
        System.out.println("===================================================================\n");
    }
}