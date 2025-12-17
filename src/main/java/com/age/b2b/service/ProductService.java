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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProductList(String keyword, ProductStatus status, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage;
        if (keyword != null && !keyword.isBlank()) {
            productPage = productRepository.findByNameContainingOrProductCodeContaining(keyword, keyword, pageable);
        } else if (status != null) {
            productPage = productRepository.findByStatus(status, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }
        return productPage.map(ProductResponseDto::from);
    }

    // 1. 상품 등록
    public Long saveProduct(ProductRequestDto dto) {
        // ★ 1. 상품코드 자동 생성 로직 (P + 년월일시분초 + 3자리난수)
        // 예: P20231217103000123
        String generatedCode = generateProductCode();

        // 혹시 모를 중복 체크 (거의 희박함)
        if (productRepository.existsByProductCode(generatedCode)) {
            throw new IllegalStateException("상품코드 생성 중 충돌이 발생했습니다. 다시 시도해주세요.");
        }

        Product product = new Product();
        product.setProductCode(generatedCode); // 생성된 코드 주입

        product.setName(dto.getName());
        product.setConsumerPrice(dto.getConsumerPrice());
        product.setSupplyPrice(dto.getSupplyPrice());
        product.setCostPrice(dto.getCostPrice());
        product.setOrigin(dto.getOrigin());
        product.setDescription(dto.getDescription());
        product.setExpiryDate(dto.getExpiryDate());

        // ★ 2. DTO에서 변환된 상태값 사용
        product.setStatus(dto.getStatus() != null ? dto.getStatus() : ProductStatus.ON_SALE);

        return productRepository.save(product).getId();
    }

    // 2. 상품 수정 (Dirty Checking)
    public void updateProduct(Long id, ProductRequestDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품이 없습니다."));
        product.setName(dto.getName());
        product.setConsumerPrice(dto.getConsumerPrice());
        product.setSupplyPrice(dto.getSupplyPrice());
        product.setCostPrice(dto.getCostPrice());
        product.setOrigin(dto.getOrigin());
        product.setDescription(dto.getDescription());

        // 상태값 수정 반영
        if(dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }
        if (dto.getExpiryDate() != null) {
            product.setExpiryDate(dto.getExpiryDate());
        }
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    private String generateProductCode() {
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomNum = ThreadLocalRandom.current().nextInt(100, 1000); // 100~999
        return "P" + dateTime + randomNum;
    }
}