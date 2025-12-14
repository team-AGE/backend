package com.age.b2b.service;

import com.age.b2b.domain.Product;
import com.age.b2b.domain.common.ProductStatus;
import com.age.b2b.dto.ProductRequestDto;
import com.age.b2b.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

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
    }

    // 3. 상품 삭제
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}