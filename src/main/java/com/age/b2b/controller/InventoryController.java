package com.age.b2b.controller;

import com.age.b2b.domain.common.StockQuality;
import com.age.b2b.dto.InboundRequestDto;
import com.age.b2b.dto.InventoryResponseDto;
import com.age.b2b.dto.StockAdjustmentDto;
import com.age.b2b.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory") // 관리자 전용 경로
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // 1. 기초 재고 등록 (입고)
    // URL: POST /api/admin/inventory/inbound
    // 역할: 새로운 Lot을 생성하고, 재고를 추가하며, 이력을 남깁니다.
    @PostMapping("/inbound")
    public ResponseEntity<Long> registerInbound(@Valid @RequestBody InboundRequestDto dto) {
        Long lotId = inventoryService.registerInbound(dto);
        // 201 Created와 함께 생성된 Lot ID를 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(lotId);
    }

    // 2. 재고 조정 (분실, 폐기, 파손 등)
    // URL: POST /api/admin/inventory/adjust
    // 역할: 기존 Lot의 수량을 증감하고, 조정 사유를 이력에 남깁니다.
    @PostMapping("/adjust")
    public ResponseEntity<Void> adjustStock(@Valid @RequestBody StockAdjustmentDto dto) {
        inventoryService.adjustStock(dto);
        // 200 OK만 반환
        return ResponseEntity.ok().build();
    }

    // 3. 재고 목록 조회 (4-1 요구사항)
    // URL: GET /api/admin/inventory/list
    // 역할: 상품 정보와 Lot 정보를 결합하고, 잔여일/자산액을 계산하여 반환합니다.
    @GetMapping("/list")
    public ResponseEntity<List<InventoryResponseDto>> getInventoryList(
            // 상품명/상품코드 검색
            @RequestParam(required = false) String keyword,
            // Lot 번호 검색
            @RequestParam(required = false) String lotNumber,
            // 재고 상태 필터링
            @RequestParam(required = false) StockQuality status) {
            List<InventoryResponseDto> list = inventoryService.getInventoryList(keyword, lotNumber, status);
        return ResponseEntity.ok(list);
    }


    // TODO: 기초 재고 삭제 기능 (Lot 삭제)은 이 기능이 완성된 후 추가 구현 예정
    // TODO: 재고 출고 (3-2. 출고 연동) 기능은 출고 로직 구현 후 연동 예정

}
