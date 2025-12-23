package com.age.b2b.repository;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.Settlement;
import com.age.b2b.dto.SettlementListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    // 이미 해당 월에 정산된 내역이 있는지 체크 (중복 정산 방지)
    boolean existsByClientAndSettlementMonth(Client client, String settlementMonth);

    // 월별 정산 조회 (배치/관리용)
    List<Settlement> findBySettlementMonth(String settlementMonth);

    /* ================= 🔥 정산관리 화면 조회용 ================= */
    @Query(
            value = "SELECT new com.age.b2b.dto.SettlementListDto(" +
                    "o.orderNumber, " +  // String
                    "o.createdAt, " +    // LocalDateTime
                    "s.totalAmount, " +  // Long
                    "'신용카드', " +      // String
                    "s.status, " +       // String
                    "s.createdAt) " +    // LocalDateTime
                    "FROM Settlement s " +
                    "JOIN s.order o " +  // Settlement 엔티티의 order 필드와 조인
                    "WHERE s.client.clientId = :clientId " +
                    "AND (:keyword IS NULL OR :keyword = '' OR o.orderNumber LIKE %:keyword% OR s.settlementNumber LIKE %:keyword%) " +
                    "ORDER BY s.createdAt ASC",
            countQuery = "SELECT COUNT(s) FROM Settlement s JOIN s.order o WHERE s.client.clientId = :clientId"
    )
    Page<SettlementListDto> findSettlementList(@Param("clientId") Long clientId, @Param("keyword") String keyword, Pageable pageable);
    }