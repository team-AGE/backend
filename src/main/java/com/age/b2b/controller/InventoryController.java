package com.age.b2b.controller;

import com.age.b2b.domain.ProductLot;
import com.age.b2b.dto.InboundRequestDto;
import com.age.b2b.dto.StockAdjustmentDto;
import com.age.b2b.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api") // 1. 전체 경로
@CrossOrigin(origins = "http://localhost:3000") // 2. 리액트 요청 허용
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    /**
     * 입고 등록 (POST)
     */
    @PostMapping("/inbound")
    public ResponseEntity<Long> registerInbound(@RequestBody InboundRequestDto dto) {
        Long lotId = inventoryService.registerInbound(dto);
        return ResponseEntity.ok(lotId);
    }

    /**
     * 전체 재고 조회 (GET)
     */
    @GetMapping("/stock_list")
    public ResponseEntity<List<ProductLot>> getAllInventory() {
        List<ProductLot> list = inventoryService.getAllInventory();
        return ResponseEntity.ok(list);
    }

    /**
     * 특정 상품 재고 조회 (GET)
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductLot>> getInventoryByProduct(@PathVariable Long productId) {
        List<ProductLot> list = inventoryService.getInventoryByProduct(productId);
        return ResponseEntity.ok(list);
    }

    /**
     * 재고 조정 (PATCH or POST)
     */
    @PatchMapping("/adjust")
    public ResponseEntity<Void> adjustStock(@RequestBody StockAdjustmentDto dto) {
        inventoryService.adjustStock(dto);
        return ResponseEntity.ok().build();
    }

}
