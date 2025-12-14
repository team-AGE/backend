package com.age.b2b.repository;

import com.age.b2b.domain.Product;
import com.age.b2b.domain.common.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 상품 코드 중복 체크용 (있으면 true 반환)
    boolean existsByProductCode(String productCode);

    // 1. 전체 조회 (최신순)
    List<Product> findAllByOrderByCreatedAtDesc();

    // 2. 검색 기능 (상품명 또는 상품코드에 검색어가 포함된 경우)
    List<Product> findByNameContainingOrProductCodeContaining(String name, String code);

    // 3. 상태 필터링 (예: 판매중인 상품만 보기)
    List<Product> findByStatus(ProductStatus status);
}