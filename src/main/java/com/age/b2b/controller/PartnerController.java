package com.age.b2b.controller;

import com.age.b2b.config.auth.PrincipalDetails;
import com.age.b2b.dto.CartDto; // DTO import 확인
import com.age.b2b.dto.OrderDto;
import com.age.b2b.dto.ProductResponseDto;
import com.age.b2b.service.CartService;
import com.age.b2b.service.OrderService;
import com.age.b2b.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    private final OrderService orderService;

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

    // 1. 주문 페이지 초기 정보
    @GetMapping("/order/init")
    public ResponseEntity<OrderDto.OrderPageData> getOrderPageData(
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return ResponseEntity.ok(orderService.getOrderPageData(principal.getClient()));
    }

    // 2. 주문 생성 (결제 전)
    @PostMapping("/order/create")
    public ResponseEntity<OrderDto.OrderResponse> createOrder(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody OrderDto.OrderRequest request
    ) {
        return ResponseEntity.ok(orderService.createOrder(principal.getClient(), request));
    }

    // 3. 결제 승인 (성공 시 호출)
    @PostMapping("/payment/toss/success")
    public ResponseEntity<String> completeTossPayment(@RequestBody Map<String, Object> body) {
        String paymentKey = (String) body.get("paymentKey");
        String orderId = (String) body.get("orderId");
        int amount = Integer.parseInt(String.valueOf(body.get("amount")));

        orderService.verifyAndCompleteTossPayment(paymentKey, orderId, amount);
        return ResponseEntity.ok("결제 완료");
    }

    // 4. 발주 목록 조회 (검색, 페이징)
    @GetMapping("/order/list")
    public ResponseEntity<Page<OrderDto.PartnerOrderListResponse>> getOrderList(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.getPartnerOrderList(
                principal.getClient(), pageable, startDate, endDate, keyword));
    }

    // 5. 발주 상세 품목 조회
    @GetMapping("/order/{orderId}/items")
    public ResponseEntity<List<OrderDto.OrderItemDetail>> getOrderItems(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(orderService.getOrderItems(orderId));
    }

    // 6. 취소 신청
    @PostMapping("/order/cancel")
    public ResponseEntity<String> requestCancelOrder(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody OrderDto.CancelRequest request
    ) {
        orderService.requestCancel(principal.getClient(), request);
        return ResponseEntity.ok("취소 신청이 완료되었습니다.");
    }
}