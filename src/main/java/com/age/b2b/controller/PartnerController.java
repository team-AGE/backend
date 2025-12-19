package com.age.b2b.controller;

import com.age.b2b.config.auth.PrincipalDetails;
import com.age.b2b.dto.CartDto; // DTO import 확인
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

    // 1. 상품 목록 조회
    @GetMapping("/product/list")
    public ResponseEntity<Page<ProductResponseDto>> getPartnerProductList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<ProductResponseDto> list = productService.getProductList(keyword, null, page);
        return ResponseEntity.ok(list);
    }

    // 2. 장바구니 담기
    @PostMapping("/cart/add")
    public ResponseEntity<String> addToCart(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody Map<String, List<String>> body
    ) {
        List<String> productCodes = body.get("productCodes");
        if (productCodes == null || productCodes.isEmpty()) {
            return ResponseEntity.badRequest().body("담을 상품이 없습니다.");
        }
        cartService.addProductsToCart(principal.getClient(), productCodes);
        return ResponseEntity.ok("장바구니에 담았습니다.");
    }

    // 3. 장바구니 조회
    @GetMapping("/cart")
    public ResponseEntity<CartDto> getCartList(@AuthenticationPrincipal PrincipalDetails principal) {
        CartDto cartDto = cartService.getCartList(principal.getClient());
        return ResponseEntity.ok(cartDto);
    }

    // 4. 수량 변경
    @PutMapping("/cart/item/{itemId}")
    public ResponseEntity<String> updateCartItem(
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> body
    ) {
        cartService.updateItemCount(itemId, body.get("count"));
        return ResponseEntity.ok("수량이 변경되었습니다.");
    }

    // 5. 장바구니 아이템 삭제
    @DeleteMapping("/cart/item/{itemId}")
    public ResponseEntity<String> deleteCartItem(@PathVariable Long itemId) {
        cartService.deleteItem(itemId);
        return ResponseEntity.ok("삭제되었습니다.");
    }
}