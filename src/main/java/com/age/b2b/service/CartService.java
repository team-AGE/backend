package com.age.b2b.service;

import com.age.b2b.domain.Cart;
import com.age.b2b.domain.CartItem;
import com.age.b2b.domain.Client;
import com.age.b2b.domain.Product;
import com.age.b2b.repository.CartRepository;
import com.age.b2b.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

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
}