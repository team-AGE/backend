package com.age.b2b.controller;

import com.age.b2b.dto.ReceivingRequestDto;
import com.age.b2b.dto.ReceivingResponseDto;
import com.age.b2b.dto.ReceivingUpdateDto;
import com.age.b2b.service.ReceivingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 입고 등록
    @PostMapping("/new")
    public ResponseEntity<String> createReceiving(@RequestBody ReceivingRequestDto dto) {
        receivingService.createReceiving(dto);
        return ResponseEntity.ok("입고 등록 완료");
    }

    // 입고 수정
    @PatchMapping("/update")
    public ResponseEntity<String> updateReceiving(@RequestBody ReceivingUpdateDto dto) {
        receivingService.updateReceiving(dto);
        return ResponseEntity.ok("입고 내역이 수정되었습니다.");
    }
    
    // 입고 삭제
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteReceiving(@RequestBody List<Long> ids) {
        receivingService.deleteReceiving(ids);
        return ResponseEntity.ok("선택한 입고 내역이 삭제되었습니다.");
    }
}