package com.age.b2b.controller;

import com.age.b2b.dto.ReceivingResponseDto;
import com.age.b2b.service.ReceivingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/receiving")
@RequiredArgsConstructor
public class ReceivingController {

    private final ReceivingService receivingService;

    // 입고 목록 조회 API
    @GetMapping("/list")
    public ResponseEntity<Page<ReceivingResponseDto>> getReceivingList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<ReceivingResponseDto> list = receivingService.getReceivingList(keyword, page);
        return ResponseEntity.ok(list);
    }
}