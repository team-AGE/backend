package com.age.b2b.repository;

import com.age.b2b.domain.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    // 특정 Lot의 이력 조회 기능을 추후 추가 가능
}