package com.age.b2b.controller;

import com.age.b2b.domain.ProductLot;
import com.age.b2b.dto.InboundRequestDto;
import com.age.b2b.dto.StockAdjustmentDto;
import com.age.b2b.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stock") // 1. 전체 경로
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
     * 전체 재고 조회 (페이징 + 검색)
     */
    @GetMapping("/list")
    public ResponseEntity<Page<ProductLot>> getAllInventory(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // InventoryService.getStockList 메서드를 호출합니다.
        return ResponseEntity.ok(inventoryService.getStockList(pageable, keyword));
    }

    /**
     * 특정 상품 재고 조회 (GET)
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductLot>> getInventoryByProduct(@PathVariable Long productId) {
        List<ProductLot> list = inventoryService.getInventoryByProduct(productId);
        return ResponseEntity.ok(list);
    }

    // 상세 조회
    @GetMapping("/detail/{id}")
    public ResponseEntity<ProductLot> getStockDetail(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getStockDetail(id));
    }

    // Lot 번호 검색
    @GetMapping("/search/lot")
    public ResponseEntity<ProductLot> getStockByLot(@RequestParam String lotNumber) {
        ProductLot lot = inventoryService.getStockByLotNumber(lotNumber);
        if (lot == null) {
            return ResponseEntity.noContent().build(); // 204 No Content (없음)
        }
        return ResponseEntity.ok(lot);
    }

    /**
     * 재고 조정 (PATCH or POST)
     */
    @PatchMapping("/adjust")
    public ResponseEntity<String> adjustStock(@RequestBody StockAdjustmentDto dto) {
        inventoryService.adjustStock(dto);
        return ResponseEntity.ok("재고 정보가 수정되었습니다.");
    }

    /**
     * 재고 삭제 (DELETE)
     */
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteStock(@RequestBody List<Long> ids) {
        inventoryService.deleteStocks(ids);
        return ResponseEntity.ok("선택한 재고가 정상적으로 삭제되었습니다.");
    }
}