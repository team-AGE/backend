package com.age.b2b.repository;

import com.age.b2b.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 상품코드 중복 체크용
    boolean existsByProductCode(String productCode);

    // 상품코드로 조회 (필요 시 사용)
    Optional<Product> findByProductCode(String productCode);
}
