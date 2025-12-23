package com.age.b2b.controller;

import com.age.b2b.dto.SettlementListDto;
import com.age.b2b.service.SettlementQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementQueryService settlementQueryService;

    @GetMapping("/list")
    public ResponseEntity<Page<SettlementListDto>> getSettlementList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword){
        Page<SettlementListDto> result = settlementQueryService.getSettlementList(page,size,keyword);
        return ResponseEntity.ok(result);
    }
}