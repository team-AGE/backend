package com.age.b2b.service;

import com.age.b2b.domain.Product;
import com.age.b2b.domain.common.ProductStatus;
import com.age.b2b.dto.ProductRequestDto;
import com.age.b2b.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Slf4j
@TestPropertySource(locations = "classpath:application-test.properties")
class ProductServiceTest {

    @Autowired ProductService productService;
    @Autowired ProductRepository productRepository;
    @Autowired EntityManager em;

    // 테스트용 DTO 생성 메서드
    private ProductRequestDto createProductDto(String code, String name) {
        return ProductRequestDto.builder()
                .productCode(code)
                .name(name)
                .consumerPrice(10000)
                .supplyPrice(8000)
                .costPrice(5000)
                .origin("한국")
                .description("몸에 좋은 비타민")
                .status(ProductStatus.ON_SALE)
                .build();
    }

    @Test
    @DisplayName("상품 등록 테스트")
    void saveProductTest() {
        // given
        ProductRequestDto dto = createProductDto("P-001", "비타민C");

        // when
        Long savedId = productService.saveProduct(dto);

        // 영속성 컨텍스트 반영
        em.flush();
        em.clear();

        // then
        Product findProduct = productRepository.findById(savedId).orElseThrow();
        log.info("등록된 상품명: {}", findProduct.getName());
        log.info("등록된 코드: {}", findProduct.getProductCode());

        assertEquals("비타민C", findProduct.getName());
        assertEquals("P-001", findProduct.getProductCode());
    }

    @Test
    @DisplayName("상품 수정 테스트")
    void updateProductTest() {
        // given (먼저 저장)
        ProductRequestDto saveDto = createProductDto("P-002", "오메가3");
        Long savedId = productService.saveProduct(saveDto);

        em.flush();
        em.clear();

        // when (수정 요청)
        ProductRequestDto updateDto = ProductRequestDto.builder()
                .name("오메가3 플러스") // 이름 변경
                .consumerPrice(15000)  // 가격 변경
                .status(ProductStatus.TEMPORARY_OUT) // 상태 변경
                .build();

        productService.updateProduct(savedId, updateDto);

        em.flush();
        em.clear();

        // then (확인)
        Product findProduct = productRepository.findById(savedId).orElseThrow();
        log.info("수정된 상품명: {}", findProduct.getName());
        log.info("수정된 상태: {}", findProduct.getStatus());

        assertEquals("오메가3 플러스", findProduct.getName());
        assertEquals(ProductStatus.TEMPORARY_OUT, findProduct.getStatus());
    }

    @Test
    @DisplayName("상품 삭제 테스트")
    void deleteProductTest() {
        // given
        ProductRequestDto dto = createProductDto("P-003", "홍삼");
        Long savedId = productService.saveProduct(dto);

        // when
        productService.deleteProduct(savedId);

        em.flush();
        em.clear();

        // then
        // 삭제해서 없으니까 에러가 터져야 성공
        assertThrows(EntityNotFoundException.class, () -> {
            productRepository.findById(savedId)
                    .orElseThrow(() -> new EntityNotFoundException("삭제됨"));
        });
        log.info("상품 삭제 완료 확인");
    }
}