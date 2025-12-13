package com.age.b2b.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "carts")
public class Cart {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long id;

    // [OneToOne LAZY] 고객사 1명당 장바구니 1개
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    // [Cascade ALL] 장바구니 삭제 시 담긴 아이템도 삭제
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    // --- 시간 설정 ---
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 마지막 수정일

    @PrePersist
    public void prePersist() { this.updatedAt = LocalDateTime.now(); }
    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "cart_items")
class CartItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private int count; // 수량
}