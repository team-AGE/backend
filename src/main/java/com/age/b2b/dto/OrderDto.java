package com.age.b2b.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
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
}