package com.age.b2b.service;

import com.age.b2b.domain.Product;
import com.age.b2b.domain.common.ProductStatus;
import com.age.b2b.dto.ProductRequestDto;
import com.age.b2b.dto.ProductResponseDto;
import com.age.b2b.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
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
@TestPropertySource(locations = "classpath:application-test.properties")
class ProductServiceTest {

    @Autowired ProductService productService;
    @Autowired ProductRepository productRepository;
    @Autowired EntityManager em;

    // 테스트용 DTO 생성 헬퍼
    private ProductRequestDto createProductDto(String name, int price) {
        return ProductRequestDto.builder()
                .productCode(null)
                .name(name)
                .consumerPrice(price + 2000)
                .supplyPrice(price)
                .costPrice(price - 3000)
                .origin("한국")
                .description("테스트용 " + name + " 입니다.")
                .status(ProductStatus.ON_SALE)
                .expiryDate(LocalDate.now().plusYears(1))
                .build();
    }

    @Test
    @DisplayName("1. 상품 등록 테스트")
    void saveProductTest() {
        System.out.println("\n================ [1. 상품 등록 시연] ================");

        // given
        String prodName = "시연용_비타민C";
        ProductRequestDto dto = createProductDto(prodName, 10000);
        System.out.println(">> 등록 요청 데이터: " + prodName + ", 공급가: 10000원");

        // when
        Long savedId = productService.saveProduct(dto);

        // 영속성 컨텍스트 초기화 (DB 반영 확인용)
        em.flush();
        em.clear();

        // then
        Product findProduct = productRepository.findById(savedId).orElseThrow();

        System.out.println(">> 등록 결과 확인:");
        System.out.println("   ID(PK): " + findProduct.getId());
        System.out.println("   상품명: " + findProduct.getName());
        System.out.println("   자동 생성된 코드: " + findProduct.getProductCode()); // 자동생성 확인
        System.out.println("   상태: " + findProduct.getStatus());

        assertEquals(prodName, findProduct.getName());
        assertNotNull(findProduct.getProductCode()); // 코드가 null이 아닌지 확인
        System.out.println("================ [등록 테스트 성공] ================\n");
    }

    @Test
    @DisplayName("2. 상품 수정 테스트")
    void updateProductTest() {
        System.out.println("\n================ [2. 상품 수정 시연] ================");

        // given
        Long savedId = productService.saveProduct(createProductDto("수정전_오메가3", 20000));
        em.flush();
        em.clear();
        System.out.println(">> 기존 상품 등록 완료 (ID: " + savedId + ")");

        // when
        System.out.println(">> 수정 요청: 이름 -> '수정후_오메가3', 상태 -> 일시품절, 원산지 -> 미국");
        ProductRequestDto updateDto = ProductRequestDto.builder()
                .name("수정후_오메가3")
                .consumerPrice(25000)
                .supplyPrice(22000)
                .costPrice(15000)
                .origin("미국") // 변경
                .description("업그레이드된 오메가3")
                .status(ProductStatus.TEMPORARY_OUT) // 변경
                .expiryDate(LocalDate.now().plusYears(2))
                .build();

        productService.updateProduct(savedId, updateDto);
        em.flush();
        em.clear();

        // then
        Product findProduct = productRepository.findById(savedId).orElseThrow();
        System.out.println(">> 수정 결과 확인:");
        System.out.println("   상품명: " + findProduct.getName());
        System.out.println("   원산지: " + findProduct.getOrigin());
        System.out.println("   상태: " + findProduct.getStatus());

        assertEquals("수정후_오메가3", findProduct.getName());
        assertEquals("미국", findProduct.getOrigin());
        assertEquals(ProductStatus.TEMPORARY_OUT, findProduct.getStatus());
        System.out.println("================ [수정 테스트 성공] ================\n");
    }

    @Test
    @DisplayName("3. 상품 삭제 테스트")
    void deleteProductTest() {
        System.out.println("\n================ [3. 상품 삭제 시연] ================");

        // given
        Long savedId = productService.saveProduct(createProductDto("삭제될_홍삼", 50000));
        em.flush();
        em.clear();
        System.out.println(">> 삭제 대상 상품 등록 완료 (ID: " + savedId + ")");

        // when
        productService.deleteProduct(savedId);
        em.flush();
        em.clear();
        System.out.println(">> 삭제 메서드 실행 완료");

        // then
        assertThrows(Exception.class, () -> {
            productRepository.findById(savedId)
                    .orElseThrow(() -> new EntityNotFoundException("삭제됨"));
        });
        System.out.println(">> 조회 시도 결과: 데이터 없음 (예외 발생 확인됨)");
        System.out.println("================ [삭제 테스트 성공] ================\n");
    }

    @Test
    @DisplayName("4. 상품 목록 조회 및 페이징")
    void getProductListTest() {
        System.out.println("\n================ [4. 목록 조회 및 페이징 시연] ================");

        // given (테스트 전용 데이터 15개 생성)
        System.out.println(">> 더미 데이터 15개 생성 중...");
        for (int i = 1; i <= 15; i++) {
            productService.saveProduct(createProductDto("테스트상품_" + i, 1000 * i));
        }
        em.flush();
        em.clear();

        // when (검색어로 조회)
        String searchKeyword = "테스트상품";
        Page<ProductResponseDto> page1 = productService.getProductList(searchKeyword, null, 0);

        // then
        System.out.println("\n[1페이지 조회 결과 (size=10)]");
        // 헤더 출력 (칸 간격 조정)
        System.out.println("-------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-15s | %-15s | %-10s | %-10s | %-20s | %-10s\n",
                "상품코드", "상품명", "공급가", "상태", "상품설명", "원산지"); // 원하시는 항목들 추가
        System.out.println("-------------------------------------------------------------------------------------------------------------------");

        // 데이터 출력
        for (ProductResponseDto p : page1.getContent()) {
            System.out.printf("%-15s | %-15s | %-10d | %-10s | %-20s | %-10s\n",
                    p.getProductCode(),
                    p.getName(),
                    p.getSupplyPrice(),
                    p.getStatus(),
                    p.getDescription(), // 상품설명 추가
                    p.getOrigin()       // 원산지 추가
            );
        }
        System.out.println("-------------------------------------------------------------------------------------------------------------------");
        System.out.println("총 페이지 수: " + page1.getTotalPages());
        System.out.println("총 데이터 수: " + page1.getTotalElements());

        assertEquals(15, page1.getTotalElements());

        System.out.println("\n================ [목록 조회 테스트 성공] ================\n");
    }
}