package com.age.b2b.service;

import com.age.b2b.domain.Cart;
import com.age.b2b.domain.CartItem;
import com.age.b2b.domain.Client;
import com.age.b2b.domain.Product;
import com.age.b2b.dto.CartDto;
import com.age.b2b.repository.CartItemRepository;
import com.age.b2b.repository.CartRepository;
import com.age.b2b.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;

    // 장바구니 담기
    public void addProductsToCart(Client client, List<String> productCodes) {
        // 1. 고객사의 장바구니 찾기 (없으면 생성)
        Cart cart = cartRepository.findByClient(client)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setClient(client);
                    return cartRepository.save(newCart);
                });

        // 2. 상품 리스트 반복 처리
        for (String code : productCodes) {
            Product product = productRepository.findByProductCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + code));

            // 이미 장바구니에 있는 상품인지 확인
            Optional<CartItem> existingItem = cart.getCartItems().stream()
                    .filter(item -> item.getProduct().getId().equals(product.getId()))
                    .findFirst();

            if (existingItem.isPresent()) {
                // 이미 있으면 수량 +1
                CartItem item = existingItem.get();
                item.setCount(item.getCount() + 1);
            } else {
                // 없으면 새로 추가
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setProduct(product);
                newItem.setCount(1);
                cart.getCartItems().add(newItem);
            }
        }
    }
    @Transactional(readOnly = true)
    public CartDto getCartList(Client client) {
        Cart cart = cartRepository.findByClient(client).orElse(null);

        if (cart == null) {
            return CartDto.builder().cartId(null).totalCount(0).items(new ArrayList<>()).build();
        }

        List<CartDto.CartItemDto> itemDtos = cart.getCartItems().stream()
                .map(item -> CartDto.CartItemDto.builder()
                        .itemId(item.getId())
                        .prodCode(item.getProduct().getProductCode())
                        .prodName(item.getProduct().getName())
                        .price(item.getProduct().getSupplyPrice())
                        .count(item.getCount())
                        .totalPrice(item.getProduct().getSupplyPrice() * item.getCount())
                        .build())
                .collect(Collectors.toList());

        return CartDto.builder()
                .cartId(cart.getId())
                .totalCount(itemDtos.size())
                .items(itemDtos)
                .build();
    }

    // 수량 변경
    public void updateItemCount(Long itemId, int count) {
        if (count <= 0) throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템이 없습니다."));
        item.setCount(count);
    }

    // 장바구니 아이템 삭제
    public void deleteItem(Long itemId) {
        cartItemRepository.deleteById(itemId);
    }
}