package com.age.b2b.controller;

import com.age.b2b.dto.OrderDetailForShipmentDto;
import com.age.b2b.dto.ShipmentCreateDto;
import com.age.b2b.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminShipmentController {

    private final ShipmentService shipmentService;

    // [출고 등록 페이지] 주문 상세 정보 프리뷰 조회
    @GetMapping("/orders/{orderId}/shipment-preview")
    public ResponseEntity<OrderDetailForShipmentDto> getShipmentPreview(@PathVariable Long orderId) {
        OrderDetailForShipmentDto dto = shipmentService.getOrderInfoForShipment(orderId);
        return ResponseEntity.ok(dto);
    }

    // [출고 등록 실행]
    @PostMapping("/shipments")
    public ResponseEntity<Long> createShipment(@RequestBody ShipmentCreateDto dto) {
        Long shipmentId = shipmentService.createShipment(dto);
        return ResponseEntity.ok(shipmentId);
    }
}