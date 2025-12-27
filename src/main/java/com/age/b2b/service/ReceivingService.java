package com.age.b2b.service;

import com.age.b2b.domain.InventoryLog;
import com.age.b2b.domain.Product;
import com.age.b2b.domain.ProductLot;
import com.age.b2b.domain.common.AdjustmentReason;
import com.age.b2b.domain.common.StockQuality;
import com.age.b2b.dto.ReceivingRequestDto;
import com.age.b2b.dto.ReceivingResponseDto;
import com.age.b2b.dto.ReceivingUpdateDto;
import com.age.b2b.repository.InventoryLogRepository;
import com.age.b2b.repository.ProductLotRepository;
import com.age.b2b.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceivingService {

    private final ProductLotRepository productLotRepository;
    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final InventoryService inventoryService;

    // 입고 목록 조회
    public Page<ReceivingResponseDto> getReceivingList(String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "id"));

        Page<ProductLot> lots;
        if (keyword != null && !keyword.isBlank()) {
            lots = productLotRepository.searchByKeyword(keyword, pageable);
        } else {
            lots = productLotRepository.findAllWithProduct(pageable);
        }

        // 엔티티 -> DTO 변환
        return lots.map(ReceivingResponseDto::from);
    }

    // 입고 등록
    @Transactional
    public void createReceiving(ReceivingRequestDto dto) {
        // 1. 상품 조회
        Product product = productRepository.findByProductCode(dto.getProductCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품코드입니다."));

        // 2. Lot 번호 생성
        String lotNumber = generateLotNumber();

        // 3. Lot 엔티티 생성 및 저장
        ProductLot lot = new ProductLot();
        lot.setProduct(product);
        lot.setLotNumber(lotNumber);
        lot.setQuantity(dto.getQty());
        lot.setExpiryDate(dto.getExpireDate());
        lot.setInboundDate(java.time.LocalDate.now());
        lot.setStockQuality(StockQuality.NORMAL);

        ProductLot savedLot = productLotRepository.save(lot);

        // 4. 재고 이력(Log) 저장
        InventoryLog log = new InventoryLog();
        log.setProductLot(savedLot);
        log.setChangeQuantity(dto.getQty());
        log.setCurrentQuantity(dto.getQty()); // 초기 수량이므로 현재 수량과 동일
        log.setReason(AdjustmentReason.INBOUND);
        log.setNote("신규 입고 등록");

        inventoryLogRepository.save(log);
    }

    // 입고 삭제
    @Transactional
    public void deleteReceiving(List<Long> ids) {
        inventoryService.deleteStocks(ids);
    }

    private String generateLotNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(100, 999);
        return "LOT-" + date + "-" + random;
    }

    // 입고 수정 메서드
    @Transactional
    public void updateReceiving(ReceivingUpdateDto dto) {
        // 1. 수정할 LOT 조회
        ProductLot lot = productLotRepository.findById(dto.getProductLotId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 입고 내역입니다."));

        // 2. 유통기한 수정
        lot.setExpiryDate(dto.getExpireDate());

        // 3. 수량 변경 확인 및 처리
        int currentQty = lot.getQuantity();
        int newQty = dto.getQty();

        if (currentQty != newQty) {
            int diff = newQty - currentQty; // 차이 계산 (예: 100 -> 120이면 +20)

            // 수량 업데이트
            lot.setQuantity(newQty);

            // 재고 이력(Log) 저장
            InventoryLog log = new InventoryLog();
            log.setProductLot(lot);
            log.setChangeQuantity(diff);
            log.setCurrentQuantity(newQty);
            log.setReason(AdjustmentReason.ADJUSTMENT); // '수정' 또는 '조정' 사유
            log.setNote(dto.getNote());

            inventoryLogRepository.save(log);
        }
    }
}