package com.age.b2b.controller;

import com.age.b2b.domain.Client;
import com.age.b2b.dto.OrderDto;
import com.age.b2b.service.ClientService;
import com.age.b2b.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ClientService clientService;
    private final OrderService orderService;

    // 1. 가입 대기 목록 조회
    @GetMapping("/requests")
    public ResponseEntity<Page<Client>> getRequestList(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<Client> list = clientService.getWaitingList(keyword, pageable);
        return ResponseEntity.ok(list);
    }

    // 2. 가입 승인/거절
    @PostMapping("/requests/status")
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> changeStatus(@RequestBody Map<String, Object> body) {
        // 리스트로 받아서 일괄 처리
        List<Integer> ids = (List<Integer>) body.get("ids");
        String status = (String) body.get("status"); // "APPROVE" or "REJECT"

        for (Integer id : ids) {
            clientService.updateApprovalStatus(Long.valueOf(id), status);
        }
        return ResponseEntity.ok("처리되었습니다.");
    }

    // 3. 이미지 불러오기 API
    @GetMapping("/image/{filename:.+}")
    public ResponseEntity<Resource> showImage(@PathVariable String filename) throws MalformedURLException {
        Resource resource = clientService.getBusinessLicenseImage(filename);

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    // 1. 전체 발주 목록 조회
    @GetMapping("/orders")
    public ResponseEntity<Page<OrderDto.AdminOrderListResponse>> getAdminOrderList(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.getAdminOrderList(pageable, startDate, endDate, keyword));
    }

    // 2. 발주 취소 (삭제 대신 상태 변경)
    @PostMapping("/orders/cancel")
    public ResponseEntity<String> cancelOrders(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        orderService.cancelOrdersByAdmin(ids);
        return ResponseEntity.ok("선택한 발주가 취소되었습니다.");
    }

    // 3. 상세 품목 조회
    @GetMapping("/orders/{orderId}/items")
    public ResponseEntity<List<OrderDto.OrderItemDetail>> getOrderItems(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderItems(orderId));
    }
}