package com.age.b2b.service;

import com.age.b2b.domain.common.ProductStatus;
import com.age.b2b.domain.Product;
import com.age.b2b.dto.ProductRegisterDto;
import com.age.b2b.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class) // Mockito 사용 설정
@Slf4j
class ProductServiceTest {
    @InjectMocks // 가짜 객체들을 주입받을 대상 (Service)
    private ProductService productService;

    @Mock // 가짜 객체로 만들 대상 (Repository)
    private ProductRepository productRepository;

    @Test
    @DisplayName("상품 등록 성공 테스트")
    void registerProduct_Success() {
        // given (준비)
        ProductRegisterDto requestDto = ProductRegisterDto.builder()
                .productCode("P-001")
                .name("비타민C")
                .consumerPrice(10000)
                .supplyPrice(8000)
                .costPrice(5000)
                .origin("한국")
                .description("피로회복에 좋은 비타민")
                .status(ProductStatus.ON_SALE)
                .build();

        // repository.save()가 호출되면, ID가 1인 가짜 Product를 리턴하도록 설정
        Product fakeProduct = new Product();
        fakeProduct.setId(1L);
        fakeProduct.setProductCode("P-001");

        // existsByProductCode 호출 시 false 리턴 (중복 없음)
        given(productRepository.existsByProductCode("P-001")).willReturn(false);
        // save 호출 시 fakeProduct 리턴
        given(productRepository.save(any())).willReturn(fakeProduct);

        // when (실행)
        Long savedId = productService.registerProduct(requestDto);

        // then (검증)
        assertThat(savedId).isEqualTo(1L); // ID가 1인지 확인
        log.info("상품 아이디: {}", savedId); // 통과했으면 로그 출력
        
        // verify: 실제로 save 메서드가 1번 호출되었는지 확인
        verify(productRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("상품 등록 실패 - 중복된 상품 코드")
    void registerProduct_Fail_Duplicate() {
        // given
        ProductRegisterDto requestDto = ProductRegisterDto.builder()
                .productCode("P-001") // 이미 있는 코드라고 가정
                .name("비타민C")
                .build();

        // existsByProductCode 호출 시 true 리턴 (중복 있음!)
        given(productRepository.existsByProductCode("P-001")).willReturn(true);

        // when & then (실행 및 예외 검증)
        // 중복이면 IllegalArgumentException이 터져야 함
        assertThrows(IllegalArgumentException.class, () -> {
            productService.registerProduct(requestDto);
        });

        // save는 실행되지 않아야 함
        verify(productRepository, times(0)).save(any());
    }
}