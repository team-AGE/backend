package com.age.b2b.repository;

import com.age.b2b.domain.ProductLot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductLotRepository extends JpaRepository<ProductLot, Long> {

    // 특정 상품의 모든 Lot 조회 (유통기한 임박순 정렬)
    List<ProductLot> findByProductIdOrderByExpiryDateAsc(Long productId);

    // Lot 번호로 조회 (중복 방지 등)
    Optional<ProductLot> findByLotNumber(String lotNumber);

    // product id로 조회
    List<ProductLot> findByProductId(Long productId);

    // [수정됨] 전체 입고 목록 조회 (최신 입고일 순 -> ID 역순)
    // Fetch Join을 사용하여 N+1 문제 방지 + 정렬 추가
    @Query("SELECT pl FROM ProductLot pl JOIN FETCH pl.product ORDER BY pl.inboundDate DESC, pl.id DESC")
    Page<ProductLot> findAllWithProduct(Pageable pageable);

    // [수정됨] 검색 기능 (결과도 최신순 정렬)
    // 검색 기능 (상품명 또는 Lot번호로 검색)
    @Query("SELECT pl FROM ProductLot pl JOIN FETCH pl.product p " +
            "WHERE p.name LIKE %:keyword% OR pl.lotNumber LIKE %:keyword%")
    Page<ProductLot> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

}