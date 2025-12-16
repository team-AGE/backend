package com.age.b2b.service;

import com.age.b2b.domain.Product;
import com.age.b2b.domain.common.ProductStatus;
import com.age.b2b.dto.ProductRequestDto;
import com.age.b2b.dto.ProductResponseDto;
import com.age.b2b.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Slf4j
@TestPropertySource(locations = "classpath:application-test.properties")
class ProductServiceTest {

    @Autowired ProductService productService;
    @Autowired ProductRepository productRepository;
    @Autowired EntityManager em;

    // 테스트용 DTO 생성 메서드 (유통기한 포함)
    private ProductRequestDto createProductDto(String code, String name) {
        return ProductRequestDto.builder()
                .productCode(code)
                .name(name)
                .consumerPrice(10000)
                .supplyPrice(8000)
                .costPrice(5000)
                .origin("한국")
                .description("몸에 좋은 " + name) // 상품설명
                .status(ProductStatus.ON_SALE)
                .expiryDate(LocalDate.now().plusYears(1))
                .build();
    }

    @Test
    @DisplayName("상품 등록 테스트")
    void saveProductTest() {
        // given
        ProductRequestDto dto = createProductDto("P-001", "비타민C");

        // when
        Long savedId = productService.saveProduct(dto);

        em.flush();
        em.clear();

        // then
        Product findProduct = productRepository.findById(savedId).orElseThrow();
        assertEquals("비타민C", findProduct.getName());
        assertEquals("P-001", findProduct.getProductCode());
    }

    @Test
    @DisplayName("상품 수정 테스트")
    void updateProductTest() {
        // given
        Long savedId = productService.saveProduct(createProductDto("P-002", "오메가3"));

        em.flush();
        em.clear();

        // when
        ProductRequestDto updateDto = ProductRequestDto.builder()
                .name("오메가3 플러스")
                .consumerPrice(15000)
                .supplyPrice(12000)
                .costPrice(8000)
                .origin("미국") // 원산지 변경
                .description("업그레이드된 오메가3")
                .status(ProductStatus.TEMPORARY_OUT)
                .expiryDate(LocalDate.now().plusYears(2))
                .build();

        productService.updateProduct(savedId, updateDto);

        em.flush();
        em.clear();

        // then
        Product findProduct = productRepository.findById(savedId).orElseThrow();
        assertEquals("오메가3 플러스", findProduct.getName());
        assertEquals("미국", findProduct.getOrigin());
        assertEquals(ProductStatus.TEMPORARY_OUT, findProduct.getStatus());
    }

    @Test
    @DisplayName("상품 삭제 테스트")
    void deleteProductTest() {
        // given
        Long savedId = productService.saveProduct(createProductDto("P-003", "홍삼"));

        // when
        productService.deleteProduct(savedId);

        em.flush();
        em.clear();

        // then
        assertThrows(EntityNotFoundException.class, () -> {
            productRepository.findById(savedId)
                    .orElseThrow(() -> new EntityNotFoundException("삭제됨"));
        });
    }

    @Test
    @DisplayName("상품 목록 조회 및 페이징 테스트 (엑셀 1-2)")
    void getProductListTest() {
        // given (데이터 15개 생성 -> 1페이지 10개, 2페이지 5개 확인용)
        for (int i = 1; i <= 15; i++) {
            String code = String.format("TEST-P-%03d", i);
            productService.saveProduct(createProductDto(code, "테스트상품_" + i));
        }

        em.flush();
        em.clear();

        // when 1: 1페이지(index 0) 조회
        Page<ProductResponseDto> page1 = productService.getProductList(null, null, 0);

        // when 2: 2페이지(index 1) 조회
        Page<ProductResponseDto> page2 = productService.getProductList(null, null, 1);

        // then
        System.out.println("\n================ [엑셀 1-2: 상품 목록 조회 (페이징)] ================");

        System.out.println("[1페이지 결과 - 10개 조회]");
        log.info("총 페이지 수: {}, 총 데이터 수: {}", page1.getTotalPages(), page1.getTotalElements());

        // 요청하신 필드 출력 확인
        for (ProductResponseDto p : page1.getContent()) {
            log.info("상품코드: {}, 상품명: {}, 공급가: {}, 상태: {}, 설명: {}, 원산지: {}",
                    p.getProductCode(),
                    p.getName(),
                    p.getSupplyPrice(),
                    p.getStatus(),
                    p.getDescription(),
                    p.getOrigin()
            );
        }

        System.out.println("\n[2페이지 결과 - 5개 조회]");
        assertEquals(10, page1.getContent().size()); // 1페이지는 10개 꽉 참
        assertEquals(5, page2.getContent().size());  // 2페이지는 나머지 5개
        assertEquals(15, page1.getTotalElements());  // 전체 개수 확인

        // 검색 테스트 (상품명 '테스트상품_1' 검색 -> 1, 10, 11, 12, 13, 14, 15 포함됨)
        Page<ProductResponseDto> searchResult = productService.getProductList("테스트상품_1", null, 0);
        System.out.println("\n[검색 테스트: '테스트상품_1']");
        log.info("검색된 상품 수: {}", searchResult.getTotalElements());

        System.out.println("===================================================================\n");
    }
}