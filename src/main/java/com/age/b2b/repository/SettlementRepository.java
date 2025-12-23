package com.age.b2b.repository;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.Settlement;
import com.age.b2b.dto.AdminSettlementListDto;
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

    boolean existsByClientAndSettlementMonth(Client client, String settlementMonth);
    List<Settlement> findBySettlementMonth(String settlementMonth);

    /* ================= 🔥 정산관리 화면 조회용(본사 - Admin) ================= */
    @Query(
            value = "SELECT new com.age.b2b.dto.AdminSettlementListDto(" +
                    "c.businessName, s.settlementNumber, o.orderNumber, o.createdAt, " + // c.businessName으로 수정
                    "s.settlementMonth, s.createdAt, s.status, p.productCode, " +
                    "p.name, oi.price, oi.count, s.totalAmount) " +
                    "FROM Settlement s " +
                    "JOIN s.order o " +
                    "JOIN o.orderItems oi " +
                    "JOIN oi.product p " +
                    "JOIN s.client c " +
                    "WHERE (:keyword IS NULL OR :keyword = '' " +
                    "OR c.businessName LIKE CONCAT('%', :keyword, '%') " +
                    "OR o.orderNumber LIKE CONCAT('%', :keyword, '%')) " +
                    "ORDER BY s.settlementNumber ASC",
            countQuery = "SELECT COUNT(s) FROM Settlement s " +
                    "JOIN s.order o JOIN o.orderItems oi JOIN s.client c " +
                    "WHERE (:keyword IS NULL OR :keyword = '' " +
                    "OR c.businessName LIKE CONCAT('%', :keyword, '%') " +
                    "OR o.orderNumber LIKE CONCAT('%', :keyword, '%'))"
    )
    Page<AdminSettlementListDto> findAllSettlementList(@Param("keyword") String keyword, Pageable pageable);

    /* ================= 🔥 정산관리 화면 조회용(고객사 - Client) ================= */
    @Query(
            value = "SELECT new com.age.b2b.dto.SettlementListDto(" +
                    "o.orderNumber, o.createdAt, s.totalAmount, '신용카드', s.status, s.createdAt) " +
                    "FROM Settlement s " +
                    "JOIN s.order o " +
                    "WHERE s.client.clientId = :clientId " +
                    "AND (:keyword IS NULL OR :keyword = '' OR o.orderNumber LIKE %:keyword% OR s.settlementNumber LIKE %:keyword%) " +
                    "ORDER BY s.createdAt ASC",
            countQuery = "SELECT COUNT(s) FROM Settlement s JOIN s.order o WHERE s.client.clientId = :clientId"
    )
    Page<SettlementListDto> findSettlementList(@Param("clientId") Long clientId, @Param("keyword") String keyword, Pageable pageable);
}