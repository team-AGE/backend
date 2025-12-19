package com.age.b2b.controller;

import com.age.b2b.dto.InboundRequestDto;
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

    /**
     * 기초 재고 등록
     */
    @PostMapping("/initial-registration")
    public ResponseEntity<String> registerInitialStock(@RequestBody InboundRequestDto dto) {
        // InventoryService의 registerInbound를 활용하여 기초 재고 등록
        inventoryService.registerInbound(dto);
        return ResponseEntity.ok("기초 재고 등록이 완료되었습니다.");
    }
}