package com.age.b2b.controller;

import com.age.b2b.domain.common.ProductStatus;
import com.age.b2b.dto.ProductRequestDto;
import com.age.b2b.dto.ProductResponseDto;
import com.age.b2b.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 상품 등록
    @PostMapping("/new")
    public ResponseEntity<String> createProduct(@RequestBody ProductRequestDto productRequestDto) {
        productService.saveProduct(productRequestDto);
        return ResponseEntity.ok("상품 등록 완료");
    }

    // 상품 목록 조회
    @GetMapping("/list")
    public ResponseEntity<Page<ProductResponseDto>> getProductList(
            @RequestParam(required = false) String keyword,    // 검색어
            @RequestParam(required = false) ProductStatus status, // 상태 필터
            @RequestParam(defaultValue = "0") int page         // 페이지 번호 (0부터 시작)
    ) {
        Page<ProductResponseDto> productList = productService.getProductList(keyword, status, page);
        return ResponseEntity.ok(productList);
    }

    // 상품 상세 조회 (수정 페이지용)
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductDetail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductDetail(id));
    }

    // 상품 수정
    @PutMapping("/{id}")
    public ResponseEntity<String> updateProduct(@PathVariable Long id, @RequestBody ProductRequestDto requestDto) {
        productService.updateProduct(id, requestDto);
        return ResponseEntity.ok("상품이 수정되었습니다.");
    }

    // 상품 삭제
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteProducts(@RequestBody List<Long> productIds) {
        // Service에 deleteProducts 메서드가 필요하지만, 반복문으로 간단히 처리
        for (Long id : productIds) {
            productService.deleteProduct(id);
        }
        return ResponseEntity.ok("선택한 상품이 삭제되었습니다.");
    }

    // 상품 코드 체크 API
    @GetMapping("/check/{code}")
    public ResponseEntity<String> checkProductCode(@PathVariable String code) {
        String productName = productService.getProductNameByCode(code);
        return ResponseEntity.ok(productName);
    }
}