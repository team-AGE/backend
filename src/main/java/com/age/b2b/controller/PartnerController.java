package com.age.b2b.controller;

import com.age.b2b.config.auth.PrincipalDetails;
import com.age.b2b.domain.Client;
import com.age.b2b.domain.ProductLot;
import com.age.b2b.dto.*;
import com.age.b2b.service.CartService;
import com.age.b2b.service.ClientService;
import com.age.b2b.service.OrderService;
import com.age.b2b.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/partner")
@RequiredArgsConstructor
public class PartnerController {

    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final ClientService clientService;  // ClientService 객체를 컨트롤러에 주입

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

    // 7. 반품 신청
    @PostMapping("/order/return")
    public ResponseEntity<String> requestReturnOrder(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody OrderDto.ReturnRequest request
    ) {
        orderService.requestReturn(principal.getClient(), request);
        return ResponseEntity.ok("반품 신청이 완료되었습니다.");
    }
    // 고객사 메인화면 신제품 조회
    @GetMapping("/product/new")
    public ResponseEntity<ProductResponseDto> getLatestProduct() {
        return ResponseEntity.ok(productService.getLatestProduct());
    }

    // 고객사 메인화면 발주현황 요약
    @GetMapping("/order/stats")
    public ResponseEntity<List<Map<String, Object>>> getOrderStats(
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        // orderService에서 상태별 카운트를 계산해오는 메서드 호출
        // 예: [{label: "결제완료", count: 5, color: "#237d31"}, ...]
        return ResponseEntity.ok(orderService.getDashboardStats(principal.getClient()));
    }

    // 8. 마이페이지 조회
    @GetMapping("/mypage")
    public ResponseEntity<ClientMyPageDto> getMyPage(
            // 로그인한 사용자의 Client 객체를 바로 가져오기 위해 사용
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        // 1. Spring Security가 UserDetailsService의 loadUserByUsername() 호출
        // 2. loadUserByUsername()에서 Repository를 사용해 DB에서 사용자 정보 조회 (Client 엔티티)
        // 3. 조회한 Client 엔티티를 기반으로 UserDetails 의 PrincipalDetails 객체 생성
        // 4. 생성된 PrincipalDetails 객체가 Security Context에 저장
        Client client = principal.getClient();  // 이미 로그인 시점에 로딩된 Client 객체
        ClientMyPageDto dto = clientService.getMyPageData(client); // 서비스에서 DTO 생성

        System.out.println("Controller 반환 데이터: " + dto); // 실제 Response에 들어가는 값
        return ResponseEntity.ok(dto);
    }

    // 9. 마이페이지 사업자 등록증 조회
    @GetMapping("/mypage/file")
    public ResponseEntity<Resource> viewBizFile(@RequestParam String filename) throws MalformedURLException {
        // 서비스 호출 -> 파일 객체(Resource) 가져오기
        // Resource는 Spring에서 파일, 클래스패스, URL 등 다양한 외부 자원을 추상화해서 다루기 위한 객체
        // MalformedURLException = 잘못된 파일 경로나 이름 예외처리
        Resource fileResource = clientService.getBusinessLicenseImage(filename);

        // 파일 확장자를 보고 타입을 자동으로 판단 (image/jpeg 등)
        String contentType = "image/jpeg"; // 기본값 설정
        try {
            // 파일 객체로부터 실제 마임타입을 추론
            File file = fileResource.getFile();
            contentType = java.nio.file.Files.probeContentType(file.toPath());
        } catch (Exception e) {
            // 예외 시 기본값 유지
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType) // ★ 이 줄이 추가되어야 이미지가 보입니다!
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(fileResource); // 파일 반환
    }

    // 10. 마이페이지 패스워드 변경
    @PutMapping("/mypage/password")
    public ResponseEntity<String> changePartnerPassword(
            // 로그인한 사용자의 Client 객체를 바로 가져오기 위해 사용
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody PasswordDto dto
    ) {

        clientService.updatePassword(principal.getClient(), dto);
        return ResponseEntity.ok("비밀번호 정보가 변경되었습니다.");
    }
    // 11. 마이페이지 정보 수정(고객사)
    @PutMapping("/mypage/profile")
    public ResponseEntity<?> updatePartnerProfile(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody ClientMyPageUpdateDto dto
    ) {
        clientService.updateProfile(principal.getClient(), dto);
        return ResponseEntity.ok("회원정보가 수정되었습니다.");
    }
}


