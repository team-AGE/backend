package com.age.b2b.service;

import com.age.b2b.domain.Product;
import com.age.b2b.dto.ProductRegisterDto;
import com.age.b2b.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Long registerProduct(ProductRegisterDto dto) {
        // 1. 중복 검사 (예시 로직)
        if (productRepository.existsByProductCode(dto.getProductCode())) {
            throw new IllegalArgumentException("이미 존재하는 상품 코드입니다.");
        }

        // 2. DTO -> Entity 변환
        Product product = new Product();
        product.setProductCode(dto.getProductCode());
        product.setName(dto.getName());
        product.setConsumerPrice(dto.getConsumerPrice());
        product.setSupplyPrice(dto.getSupplyPrice());
        product.setCostPrice(dto.getCostPrice());
        product.setOrigin(dto.getOrigin());
        product.setDescription(dto.getDescription());
        product.setStatus(dto.getStatus());

        // 3. 저장
        Product savedProduct = productRepository.save(product);
        return savedProduct.getId();
    }
}
