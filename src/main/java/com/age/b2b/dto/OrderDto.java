package com.age.b2b.dto;

import lombok.*;

import java.util.List;

public class OrderDto {

    // 1. 주문 페이지 진입 시 보여줄 정보
    @Getter @Builder
    public static class OrderPageData {
        private String ownerName;
        private String phone;
        private String zipCode;
        private String address;
        private String detailAddress;
    }

    // 2. 주문 생성 요청
    @Getter @Setter
    public static class OrderRequest {
        private List<Long> cartItemIds;
        private String receiverName;
        private String receiverPhone;
        private String address;
        private String memo;

        private List<OrderItemRequest> orderItems;
    }

    @Getter @Setter
    public static class OrderItemRequest {
        private Long productId;
        private int count;
    }

    // 3. 주문 생성 응답 (결제창용)
    @Getter @Builder
    public static class OrderResponse {
        private String orderNumber;
        private String orderName;
        private int totalAmount;
        private String buyerName;
        private String buyerEmail;
        private String buyerTel;
    }

    // 4. 파트너 주문 목록 조회용 DTO
    @Getter @Builder @AllArgsConstructor
    public static class PartnerOrderListResponse {
        private Long orderId;           // PK (모달 조회용)
        private String orderNumber;     // 주문번호
        private String createdAt;       // 발주일자 (String 포맷팅)
        private String repProductCode;  // 대표 상품 코드
        private String repProductName;  // 대표 상품명
        private int itemCount;          // 외 N건 계산용
        private int repProductPrice;    // 대표 상품 공급가 (목록 표기용, 필요시)
        private int totalQuantity;      // 총 수량
        private int totalAmount;        // 총 금액
        private String status;          // 주문 상태 (한글 변환은 프론트에서 권장)
        private String deliveryDate;    // 배송완료일자 (없으면 null)
    }

    // 본사 관리자용 주문 목록 DTO
    @Getter @Builder @AllArgsConstructor
    public static class AdminOrderListResponse {
        private Long orderId;
        private String orderNumber;
        private String clientName;
        private String createdAt;
        private String repProductCode;
        private String repProductName;
        private int itemCount;
        private int totalAmount;
        private String status;
    }

    // 5. 주문 상세 품목 DTO
    @Getter @Builder @AllArgsConstructor
    public static class OrderItemDetail {
        private String productCode;
        private String productName;
        private int price;
        private int count;
        private int totalPrice;
    }

    // 취소 신청 요청 DTO
    @Getter @Setter
    @NoArgsConstructor
    public static class CancelRequest {
        private List<Long> orderIds; // 취소할 주문 ID 목록 (여러 개일 수 있음)
        private String cancelReason; // 사유 (예: CHANGE_MIND)
        private String cancelDetail; // 상세 사유
    }
}