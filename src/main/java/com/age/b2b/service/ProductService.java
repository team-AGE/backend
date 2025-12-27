package com.age.b2b.service;

import com.age.b2b.domain.Product;
import com.age.b2b.domain.common.ProductStatus;
import com.age.b2b.dto.ProductRequestDto;
import com.age.b2b.dto.ProductResponseDto;
import com.age.b2b.repository.CartItemRepository;
import com.age.b2b.repository.OrderItemRepository;
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
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public String getProductNameByCode(String code) {
        return productRepository.findByProductCode(code)
                .map(Product::getName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품코드입니다."));
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다. id=" + id));
        return ProductResponseDto.from(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProductList(String keyword, ProductStatus status, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "id"));
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
    // 신상품 반환 로직
    @Transactional(readOnly = true)
    public ProductResponseDto getLatestProduct() {
        return productRepository.findFirstByOrderByCreatedAtDesc()
                .map(ProductResponseDto::from)
                .orElseThrow(() -> new IllegalArgumentException("등록된 상품이 없습니다."));
    }
    // 1. 상품 등록
    public Long saveProduct(ProductRequestDto dto) {
        // 1. 상품코드 자동 생성 로직 (P + 년월일시분초 + 3자리난수)
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

        // 2. DTO에서 변환된 상태값 사용
        product.setStatus(dto.getStatus() != null ? dto.getStatus() : ProductStatus.ON_SALE);

        return productRepository.save(product).getId();
    }

    // 2. 상품 수정
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
        // (1) 주문 내역 확인: 이미 팔린 상품은 삭제 불가
        if (orderItemRepository.existsByProductId(id)) {
            throw new IllegalStateException("이미 주문 이력이 있는 상품이 포함되어 있습니다.\n상품 수정에서 상품상태를 변경해주세요.");
        }

        // (2) 장바구니 내역 삭제
        cartItemRepository.deleteByProductId(id);

        // (3) 상품 삭제
        productRepository.deleteById(id);
    }

    private String generateProductCode() {
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomNum = ThreadLocalRandom.current().nextInt(100, 1000); // 100~999
        return "P" + dateTime + randomNum;
    }
}