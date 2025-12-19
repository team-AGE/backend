package com.age.b2b.service;

import com.age.b2b.domain.InventoryLog;
import com.age.b2b.domain.Product;
import com.age.b2b.domain.ProductLot;
import com.age.b2b.domain.common.AdjustmentReason;
import com.age.b2b.domain.common.StockQuality;
import com.age.b2b.dto.InboundRequestDto;
import com.age.b2b.dto.StockAdjustmentDto;
import com.age.b2b.repository.InventoryLogRepository;
import com.age.b2b.repository.ProductLotRepository;
import com.age.b2b.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final ProductLotRepository productLotRepository;
    private final InventoryLogRepository inventoryLogRepository;

    /**
     * [본사] 상품 입고 처리 (새로운 Lot 생성)
     */
    public Long registerInbound(InboundRequestDto dto) {
        // 1. 화면에서 입력한 상품코드로 실제 상품이 존재하는지 확인
        Product product = productRepository.findByProductCode(dto.getProductCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품코드입니다."));

        // 2. 화면 요구사항: Lot 번호 자동 생성
        String lotNumber = generateLotNumber();

        // 3. 재고(Lot) 엔티티 생성 및 데이터 매핑
        ProductLot lot = new ProductLot();
        lot.setProduct(product);
        lot.setLotNumber(lotNumber); // 서버에서 생성한 번호 사용
        lot.setQuantity(dto.getQuantity());
        lot.setExpiryDate(dto.getExpiryDate());
        lot.setInboundDate(dto.getInboundDate());

        // (선택) 원산지 정보 처리 - ProductLot에 origin 필드가 있다면 주입
        // lot.setOrigin(dto.getOrigin());

        // 4. 유통기한 기준 상태 자동 판별 (3개월 미만 시 주의재고)
        if (dto.getExpiryDate().isBefore(LocalDate.now().plusMonths(3))) {
            lot.setStockQuality(StockQuality.CAUTION);
        } else {
            lot.setStockQuality(StockQuality.NORMAL);
        }

        ProductLot savedLot = productLotRepository.save(lot);

        // 5. 재고 이력(Log) 저장 (사유: 기초 등록)
        saveInventoryLog(savedLot, dto.getQuantity(), AdjustmentReason.INBOUND, "기초 재고 등록");

        return savedLot.getId();
    }

    // Lot 번호 생성기 (규칙: LOT-날짜-난수)
    private String generateLotNumber() {
        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(100, 999);
        return "LOT-" + date + "-" + random;
    }

    /**
     * [본사] 재고 조정 (파손, 분실, 폐기 등)
     */
    public void adjustStock(StockAdjustmentDto dto) {
        // 1. Lot 조회
        ProductLot lot = productLotRepository.findById(dto.getProductLotId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Lot입니다."));

        // 2. 수량 변경 검증 (출고/폐기 시 재고 부족 체크)
        int newQuantity = lot.getQuantity() + dto.getChangeQuantity();
        if (newQuantity < 0) {
            throw new IllegalStateException("재고가 부족하여 처리할 수 없습니다. 현재: " + lot.getQuantity());
        }

        // 3. 수량 업데이트
        lot.setQuantity(newQuantity);

        // 4. 이력(Log) 저장
        saveInventoryLog(lot, dto.getChangeQuantity(), dto.getReason(), dto.getNote());
    }

    // (내부 메서드) 이력 저장 공통화
    private void saveInventoryLog(ProductLot lot, int changeQty, AdjustmentReason reason, String note) {
        InventoryLog log = new InventoryLog();
        log.setProductLot(lot);
        log.setChangeQuantity(changeQty);
        log.setCurrentQuantity(lot.getQuantity()); // 변경 후 최종 수량 스냅샷
        log.setReason(reason);
        log.setNote(note);

        inventoryLogRepository.save(log);
    }
}
