package com.age.b2b.controller;

import com.age.b2b.dto.ProductRequestDto;
import com.age.b2b.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}