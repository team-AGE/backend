package com.age.b2b.controller;

import com.age.b2b.dto.AdminSettlementListDto;
import com.age.b2b.dto.SettlementListDto;
import com.age.b2b.service.SettlementQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementQueryService settlementQueryService;

    // 본사 관리자용
    @GetMapping
    public ResponseEntity<Page<AdminSettlementListDto>> getAllSettlements(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(settlementQueryService.getAdminSettlementList(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                keyword));
    }

    // 고객사용
    @GetMapping("/list")
    public ResponseEntity<Page<SettlementListDto>> getSettlementList(
            Principal principal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword){

        String username = principal.getName();
        Page<SettlementListDto> result = settlementQueryService.getSettlementList(username, page, size, keyword);
        return ResponseEntity.ok(result);
    }
}