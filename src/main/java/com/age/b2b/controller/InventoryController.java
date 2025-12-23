package com.age.b2b.controller;

import com.age.b2b.domain.ProductLot;
import com.age.b2b.dto.InboundRequestDto;
import com.age.b2b.dto.InventoryResponseDto; // ★ Import 확인
import com.age.b2b.dto.StockAdjustmentDto;
import com.age.b2b.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // 기초 재고 등록
    @PostMapping("/initial-registration")
    public ResponseEntity<String> registerInitialStock(@RequestBody InboundRequestDto dto) {
        inventoryService.registerInbound(dto);
        return ResponseEntity.ok("기초 재고 등록이 완료되었습니다.");
    }

    /**
     * 전체 재고 조회 (DTO 반환)
     */
    @GetMapping("/list")
    public ResponseEntity<List<InventoryResponseDto>> getAllInventory() {
        // 서비스 메서드 이름도 getInventoryList()로 바뀜
        List<InventoryResponseDto> list = inventoryService.getInventoryList();
        return ResponseEntity.ok(list);
    }
    /**
     * 특정 상품 재고 조회 (Entity 반환 유지)
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductLot>> getInventoryByProduct(@PathVariable Long productId) {
        List<ProductLot> list = inventoryService.getInventoryByProduct(productId);
        return ResponseEntity.ok(list);
    }

    /**
     * 재고 조정
     */
    @PatchMapping("/adjust")
    public ResponseEntity<Void> adjustStock(@RequestBody StockAdjustmentDto dto) {
        inventoryService.adjustStock(dto);
        return ResponseEntity.ok().build();
    }
}