package com.age.b2b.controller;

import com.age.b2b.domain.Product;
import com.age.b2b.domain.common.ProductStatus;
import com.age.b2b.dto.ProductRequestDto;
import com.age.b2b.dto.ProductResponseDto;
import com.age.b2b.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    // 1. 상품 등록 (관리자 전용)
    // URL : POST /api/admin/products
    @PostMapping("/create_product")
    public ResponseEntity<Long> registerProduct(@Valid @RequestBody ProductRequestDto requestDto) {
        Long savedId = productService.saveProduct(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedId);
    }

    // 2. 상품 수정 (관리자 전용)
    // URL: PUT /edit_product/{productId}
    @PutMapping("/edit_product/{productId}")
    public ResponseEntity<Void> updateProduct(@PathVariable Long productId,
                                              @Valid @RequestBody ProductRequestDto requestDto) {
        productService.updateProduct(productId, requestDto);
        return ResponseEntity.ok().build();
    }

    // 3. 상품 목록 조회 및 검색 (관리자/고객사 공용)
    // URL: GET /api/products
    @GetMapping("/product_list")
    public ResponseEntity<List<ProductResponseDto>> getProductList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductStatus status) {

        List<ProductResponseDto> list = productService.getProductList(keyword, status);
        return ResponseEntity.ok(list);
    }

    // 4. 상품 삭제 (관리자 전용)
    @DeleteMapping("/")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        // 204 No Content 또는 200 OK를 반환할 수 있으나, 일반적으로 200 OK를 사용함
        return ResponseEntity.ok().build();
    }
}
