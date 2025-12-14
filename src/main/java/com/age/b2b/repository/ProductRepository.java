package com.age.b2b.repository;

import com.age.b2b.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 상품 코드 중복 체크용 (있으면 true 반환)
    boolean existsByProductCode(String productCode);
}