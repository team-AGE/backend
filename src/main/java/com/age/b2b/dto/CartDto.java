package com.age.b2b.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {
    private Long cartId;
    private int totalCount; // 전체 상품 종류 수
    private List<CartItemDto> items;

    @Getter @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDto {
        private Long itemId;      // 장바구니 아이템 PK
        private String prodCode;  // 상품코드
        private String prodName;  // 상품명
        private int price;        // 공급가
        private int count;        // 수량
        private int totalPrice;   // 총 금액 (단가 * 수량)
    }
}