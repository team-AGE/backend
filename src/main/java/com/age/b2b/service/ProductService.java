package com.age.b2b.service;

import com.age.b2b.domain.Product;
import com.age.b2b.domain.common.ProductStatus;
import com.age.b2b.dto.ProductRequestDto;
import com.age.b2b.dto.ProductResponseDto;
import com.age.b2b.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProductList(String keyword, ProductStatus status, int page) {

        // 1. 페이징 설정 (한 페이지당 10개, 등록일 역순 정렬)
        // page: 프론트에서 넘어온 페이지 번호 (0 = 1페이지)
        // size: 10
        // Sort: createdAt 내림차순 (최신순)
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> productPage;

        // 2. 검색 조건에 따른 조회 분기
        if (keyword != null && !keyword.isBlank()) {
            // 검색어 포함 조회
            productPage = productRepository.findByNameContainingOrProductCodeContaining(keyword, keyword, pageable);
        } else if (status != null) {
            // 상태값 필터 조회
            productPage = productRepository.findByStatus(status, pageable);
        } else {
            // 전체 조회
            productPage = productRepository.findAll(pageable);
        }

        // 3. 엔티티(Page<Product>) -> DTO(Page<ProductResponseDto>) 변환
        // map() 함수를 쓰면 내부 내용물만 쏙쏙 변환해줍니다.
        return productPage.map(ProductResponseDto::from);
    }

    // 1. 상품 등록
    public Long saveProduct(ProductRequestDto dto) {
        // 중복 체크
        if (productRepository.existsByProductCode(dto.getProductCode())) {
            throw new IllegalStateException("이미 존재하는 상품코드입니다.");
        }

        Product product = new Product();
        product.setProductCode(dto.getProductCode());
        product.setName(dto.getName());
        product.setConsumerPrice(dto.getConsumerPrice());
        product.setSupplyPrice(dto.getSupplyPrice());
        product.setCostPrice(dto.getCostPrice());
        product.setOrigin(dto.getOrigin());
        product.setDescription(dto.getDescription());
        product.setStatus(ProductStatus.ON_SALE); // 기본값 판매중

        product.setExpiryDate(dto.getExpiryDate());

        return productRepository.save(product).getId();
    }

    // 2. 상품 수정 (Dirty Checking)
    public void updateProduct(Long id, ProductRequestDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품이 없습니다."));

        // 상품 정보 변경
        product.setName(dto.getName());
        product.setConsumerPrice(dto.getConsumerPrice());
        product.setSupplyPrice(dto.getSupplyPrice());
        product.setCostPrice(dto.getCostPrice());
        product.setOrigin(dto.getOrigin());
        product.setDescription(dto.getDescription());
        product.setStatus(dto.getStatus());

        if (dto.getExpiryDate() != null) {
            product.setExpiryDate(dto.getExpiryDate());
        }
    }

    // 3. 상품 삭제
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}