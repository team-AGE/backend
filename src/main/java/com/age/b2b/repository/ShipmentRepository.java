package com.age.b2b.repository;

import com.age.b2b.domain.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    // 검색 기능 (출고번호, 주문번호, 상품명 매칭)
    @Query("SELECT s FROM Shipment s " +
            "JOIN FETCH s.order o " +
            "JOIN FETCH o.client c " +
            "WHERE (:keyword IS NULL OR :keyword = '' " +
            "   OR s.shipmentNumber LIKE %:keyword% " +
            "   OR o.orderNumber LIKE %:keyword% " +
            "   OR EXISTS (SELECT i FROM OrderItem i WHERE i.order = o AND i.product.name LIKE %:keyword%))")
    Page<Shipment> searchShipments(@Param("keyword") String keyword, Pageable pageable);
}