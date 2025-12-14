package com.age.b2b.repository;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    // 이미 해당 월에 정산된 내역이 있는지 체크 (중복 정산 방지)
    boolean existsByClientAndSettlementMonth(Client client, String settlementMonth);

    // 정산 목록 조회
    List<Settlement> findBySettlementMonth(String settlementMonth);
}