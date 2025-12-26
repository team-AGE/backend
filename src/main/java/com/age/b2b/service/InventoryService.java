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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
        // 1. 상품 존재 확인
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 2. Lot 생성
        ProductLot lot = new ProductLot();
        lot.setProduct(product);
        lot.setLotNumber(dto.getLotNumber());
        lot.setQuantity(dto.getQuantity()); // 초기 수량
        lot.setExpiryDate(dto.getExpiryDate());
        lot.setInboundDate(dto.getInboundDate());

        // 유통기한에 따른 상태 자동 설정
        updateStockQuality(lot);

        ProductLot savedLot = productLotRepository.save(lot);

        // 3. 이력(Log) 저장
        saveInventoryLog(savedLot, dto.getQuantity(), AdjustmentReason.INBOUND, "최초 입고");

        return savedLot.getId();
    }

    /**
     * [본사] 전체 재고 현황 조회 (List 반환 - 테스트용)
     */
    @Transactional(readOnly = true)
    public List<ProductLot> getAllInventory() {
        List<ProductLot> list = productLotRepository.findAll();
        System.out.println(">>> DB에서 가져온 데이터 개수: " + list.size());
        return list;
    }

    /**
     * [본사] 특정 상품 재고 현황 조회
     */
    @Transactional(readOnly = true)
    public List<ProductLot> getInventoryByProduct(Long productId) {
        return productLotRepository.findByProductId(productId);
    }

    /**
     * [본사] 재고 목록 조회 (검색 + 페이징)
     */
    @Transactional(readOnly = true)
    public Page<ProductLot> getStockList(Pageable pageable, String keyword) {
        return productLotRepository.searchStock(keyword, pageable);
    }

    /**
     * [본사] 재고 상세 조회 (ID로 조회)
     */
    @Transactional(readOnly = true)
    public ProductLot getStockDetail(Long id) {
        return productLotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 재고 정보를 찾을 수 없습니다."));
    }

    /**
     * [본사] Lot 번호로 재고 조회 (검색용)
     */
    @Transactional(readOnly = true)
    public ProductLot getStockByLotNumber(String lotNumber) {
        return productLotRepository.findByLotNumber(lotNumber)
                .orElse(null);
    }

    /**
     * [본사] 재고 조정 (수량, 유통기한, 상태 변경 등)
     */
    public void adjustStock(StockAdjustmentDto dto) {
        // 1. Lot 조회
        ProductLot lot = productLotRepository.findById(dto.getProductLotId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Lot입니다."));

        // 2. 수량 변경 검증 & 적용
        if (dto.getChangeQuantity() != 0) {
            int newQuantity = lot.getQuantity() + dto.getChangeQuantity();
            if (newQuantity < 0) {
                throw new IllegalStateException("재고가 부족하여 처리할 수 없습니다. 현재: " + lot.getQuantity());
            }
            lot.setQuantity(newQuantity);
        }

        // 3. 유통기한 변경 & 상태 재계산
        if (dto.getExpiryDate() != null) {
            lot.setExpiryDate(dto.getExpiryDate());
            // 변경된 유통기한 기준으로 상태(정상/주의/폐기) 업데이트
            updateStockQuality(lot);
        }

        // 4. 이력(Log) 저장
        // 수량 변동이 있거나 유통기한이 변경되었을 때 로그 저장
        if (dto.getChangeQuantity() != 0 || dto.getExpiryDate() != null) {
            String note = dto.getNote();
            if (dto.getExpiryDate() != null) {
                note = (note == null ? "" : note) + " [유통기한 변경]";
            }
            saveInventoryLog(lot, dto.getChangeQuantity(), dto.getReason(), note);
        }
    }

    /**
     * [본사] 재고 삭제 (다중 삭제)
     */
    public void deleteStocks(List<Long> lotIds) {
        for (Long lotId : lotIds) {
            // 1. 해당 재고의 이력(로그) 모두 삭제 (FK 제약조건 해결)
            inventoryLogRepository.deleteByProductLotId(lotId);

            // 2. 재고(Lot) 삭제
            productLotRepository.deleteById(lotId);
        }
    }

    // --- 내부 헬퍼 메서드 ---

    // 유통기한에 따른 재고 등급 계산
    private void updateStockQuality(ProductLot lot) {
        if (lot.getExpiryDate() == null) return;

        LocalDate today = LocalDate.now();
        LocalDate expiry = lot.getExpiryDate();

        long diffDays = ChronoUnit.DAYS.between(today, expiry);

        if (diffDays < 0) {
            lot.setStockQuality(StockQuality.DISPOSAL); // 폐기대상
        } else if (diffDays < 90) {
            lot.setStockQuality(StockQuality.CAUTION);  // 주의재고
        } else if (diffDays < 365) {
            lot.setStockQuality(StockQuality.MANAGED);  // 관리재고
        } else {
            lot.setStockQuality(StockQuality.NORMAL);   // 정상재고
        }
    }

    // 이력 저장 공통화
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