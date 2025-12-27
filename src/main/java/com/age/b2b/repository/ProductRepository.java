package com.age.b2b.repository;

import com.age.b2b.domain.Product;
import com.age.b2b.domain.common.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 상품 코드 중복 체크용 (있으면 true 반환)
    boolean existsByProductCode(String productCode);

    Optional<Product> findByProductCode(String productCode);
    Optional<Product> findFirstByOrderByCreatedAtDesc();
    // 1. 전체 조회 (정렬은 Pageable에서 처리)
    Page<Product> findAll(Pageable pageable);

    // 2. 검색 기능 (상품명 또는 상품코드)
    Page<Product> findByNameContainingOrProductCodeContaining(String name, String code, Pageable pageable);

    // 3. 상태 필터링
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
}