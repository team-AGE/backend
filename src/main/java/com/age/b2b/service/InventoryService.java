package com.age.b2b.service;

import com.age.b2b.domain.InventoryLog;
import com.age.b2b.domain.Product;
import com.age.b2b.domain.ProductLot;
import com.age.b2b.domain.common.AdjustmentReason;
import com.age.b2b.domain.common.StockQuality;
import com.age.b2b.dto.InboundRequestDto;
import com.age.b2b.dto.InventoryResponseDto;
import com.age.b2b.dto.StockAdjustmentDto;
import com.age.b2b.repository.InventoryLogRepository;
import com.age.b2b.repository.ProductLotRepository;
import com.age.b2b.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final ProductLotRepository productLotRepository;
    private final InventoryLogRepository inventoryLogRepository;

    /**
     * [본사] 상품 입고 및 기초재고 등록
     */
    @Transactional
    public Long registerInbound(InboundRequestDto dto) {
        // 1. 상품 존재 확인
        Product product = productRepository.findByProductCode(dto.getProductCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품코드입니다: " + dto.getProductCode()));

        // ★ [수정] 사용자가 입력한 원산지가 있으면, 상품 정보를 업데이트합니다!
        if (dto.getOrigin() != null && !dto.getOrigin().isBlank()) {
            product.setOrigin(dto.getOrigin()); // "캐나다" -> "한국"으로 변경됨
        }

        // 2. Lot 번호 생성
        String lotNumber = generateLotNumber();

        // 3. Entity 생성
        ProductLot lot = new ProductLot();
        lot.setProduct(product);
        lot.setProductCode(product.getProductCode());
        lot.setLotNumber(lotNumber);
        lot.setQuantity(dto.getQuantity());
        lot.setExpiryDate(dto.getExpiryDate());
        lot.setInboundDate(dto.getInboundDate() != null ? dto.getInboundDate() : LocalDate.now());

        // 4. 상태 판별 (3개월 미만 주의)
        if (dto.getExpiryDate().isBefore(LocalDate.now().plusMonths(3))) {
            lot.setStockQuality(StockQuality.CAUTION);
        } else {
            lot.setStockQuality(StockQuality.NORMAL);
        }

        ProductLot savedLot = productLotRepository.save(lot);

        // 5. 로그 저장
        saveInventoryLog(savedLot, dto.getQuantity(), AdjustmentReason.INBOUND, "입고/기초등록");

        return savedLot.getId();
    }

    private String generateLotNumber() {
        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(1000, 10000);
        return "LOT-" + date + "-" + random;
    }

    /**
     * [본사] 전체 재고 현황 조회 (DTO 반환)
     */
    @Transactional(readOnly = true)
    public List<InventoryResponseDto> getInventoryList() {
        List<ProductLot> lots = productLotRepository.findAll();

        // Entity(원본) -> DTO(화면용) 변환
        return lots.stream()
                .map(InventoryResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * [본사] 특정 상품 재고 조회 (빠져있던 메서드 추가됨)
     */
    @Transactional(readOnly = true)
    public List<ProductLot> getInventoryByProduct(Long productId) {
        return productLotRepository.findByProductId(productId);
    }

    /**
     * [본사] 재고 조정
     */
    public void adjustStock(StockAdjustmentDto dto) {
        ProductLot lot = productLotRepository.findById(dto.getProductLotId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Lot입니다."));

        int newQuantity = lot.getQuantity() + dto.getChangeQuantity();
        if (newQuantity < 0) {
            throw new IllegalStateException("재고 부족. 현재: " + lot.getQuantity());
        }

        lot.setQuantity(newQuantity);
        saveInventoryLog(lot, dto.getChangeQuantity(), dto.getReason(), dto.getNote());
    }

    private void saveInventoryLog(ProductLot lot, int changeQty, AdjustmentReason reason, String note) {
        InventoryLog log = new InventoryLog();
        log.setProductLot(lot);
        log.setChangeQuantity(changeQty);
        log.setCurrentQuantity(lot.getQuantity());
        log.setReason(reason);
        log.setNote(note);
        inventoryLogRepository.save(log);
    }
}