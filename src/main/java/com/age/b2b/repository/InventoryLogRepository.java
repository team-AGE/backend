package com.age.b2b.repository;

import com.age.b2b.domain.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    // 특정 Lot의 모든 로그 삭제
    @Modifying
    @Query("DELETE FROM InventoryLog l WHERE l.productLot.id = :lotId")
    void deleteByProductLotId(@Param("lotId") Long lotId);

    // 특정 Lot의 로그가 있는지 확인용
    List<InventoryLog> findByProductLotId(Long lotId);
}