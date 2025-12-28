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

        private String zipCode;       // 우편번호
        private String address;       // 주소
        private String detailAddress; // 상세주소
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
    @NoArgsConstructor
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

    // 반품 신청 요청 DTO
    @Getter @Setter
    @NoArgsConstructor
    public static class ReturnRequest {
        private List<Long> orderIds;   // 반품할 주문 ID 목록
        private String returnReason;   // 반품 사유 (CHANGE_MIND, DEFECTIVE 등)
        private String returnDetail;   // 상세 사유
    }

    // 본사 반품 관리 목록 조회용 DTO
    @Getter @Builder @AllArgsConstructor
    public static class AdminReturnListResponse {
        private Long orderId;           // PK
        private String orderNumber;     // 발주번호
        private String orderDate;       // 발주일자
        private String productCode;     // 상품코드
        private String productName;     // 상품명
        private int supplyPrice;        // 공급가
        private int quantity;           // 수량
        private int totalAmount;        // 총 금액
        private String returnReason;    // 반품 사유
        private String status;          // 상태
        private String returnRequestDate; // 반품 요청일자
        private String statusDate;      // 상태처리일자 (최근 수정일)
    }

    // 본사 취소 관리 목록 조회용 DTO
    @Getter @Builder @AllArgsConstructor
    public static class AdminCancelListResponse {
        private Long orderId;
        private String orderNumber;
        private String orderDate;       // 발주일자 (yyyy-MM-dd)

        private String productName;     // 상품명
        private String productCode;     // 상품코드
        private int supplyPrice;        // 공급가
        private int quantity;           // 수량
        private int totalAmount;        // 총금액 (공급가 * 수량)

        private String cancelReason;    // 취소사유
        private String status;          // 상태 (취소요청, 취소완료 등)

        private String cancelRequestDate;    // 취소 요청일
        private String statusProcessingDate; // 상태 처리일 (승인/거절일)
    }
}