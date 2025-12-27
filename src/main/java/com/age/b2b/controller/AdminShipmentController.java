package com.age.b2b.controller;

import com.age.b2b.dto.OrderDetailForShipmentDto;
import com.age.b2b.dto.ShipmentCreateDto;
import com.age.b2b.dto.ShipmentListResponseDto;
import com.age.b2b.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 출고 목록 조회
    @GetMapping("/shipments/list")
    public ResponseEntity<Page<ShipmentListResponseDto>> getShipmentList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(shipmentService.getShipmentList(keyword, page));
    }

    // 출고 삭제
    @DeleteMapping("/shipments/delete")
    public ResponseEntity<String> deleteShipments(@RequestBody List<Long> shipmentIds) {
        shipmentService.deleteShipments(shipmentIds);
        return ResponseEntity.ok("선택한 출고 내역이 삭제(취소)되었습니다.");
    }
}