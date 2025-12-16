package com.age.b2b.repository;

import com.age.b2b.domain.ProductLot;
import com.age.b2b.domain.common.StockQuality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductLotRepository extends JpaRepository<ProductLot, Long> {

    // 특정 상품의 모든 Lot 조회 (유통기한 임박순 정렬)
    List<ProductLot> findByProductIdOrderByExpiryDateAsc(Long productId);
    // (추가) 상품명 또는 상품코드로 검색
    List<ProductLot> findByProduct_NameContainingOrProduct_ProductCodeContaining(
            String nameKeyword, String codeKeyword);

    //  Lot 번호로 검색
    List<ProductLot> findByLotNumberContaining(String lotNumber);

    //  재고 상태로 필터링 (StockQuality Enum 사용)
    List<ProductLot> findByStockQuality(StockQuality status);

    // Lot 번호로 조회 (중복 방지 등)
    Optional<ProductLot> findByLotNumber(String lotNumber);
}