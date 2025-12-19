package com.age.b2b.controller;

import com.age.b2b.config.auth.PrincipalDetails;
import com.age.b2b.dto.ProductResponseDto;
import com.age.b2b.service.CartService;
import com.age.b2b.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/partner")
@RequiredArgsConstructor
public class PartnerController {

    private final ProductService productService;
    private final CartService cartService;

    // 1. 상품 목록 조회 (고객사용)
    // status 파라미터를 받지 않거나 null로 넘겨서 "모든 상태"의 상품을 조회함
    @GetMapping("/product/list")
    public ResponseEntity<Page<ProductResponseDto>> getPartnerProductList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page
    ) {
        // status에 null을 전달하면 ProductService에서 전체 조회함
        Page<ProductResponseDto> list = productService.getProductList(keyword, null, page);
        return ResponseEntity.ok(list);
    }

    // 2. 장바구니 담기
    @PostMapping("/cart/add")
    public ResponseEntity<String> addToCart(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody Map<String, List<String>> body // { "productCodes": ["P001", "P002"] }
    ) {
        List<String> productCodes = body.get("productCodes");
        if (productCodes == null || productCodes.isEmpty()) {
            return ResponseEntity.badRequest().body("담을 상품이 없습니다.");
        }

        cartService.addProductsToCart(principal.getClient(), productCodes);
        return ResponseEntity.ok("장바구니에 담았습니다.");
    }
}