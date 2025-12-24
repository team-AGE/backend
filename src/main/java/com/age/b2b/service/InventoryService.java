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

        // 유통기한에 따른 상태 자동 설정 (단순 로직 예시)
        if (dto.getExpiryDate().isBefore(LocalDate.now().plusMonths(3))) {
            lot.setStockQuality(StockQuality.CAUTION); // 3개월 미만 주의
        } else {
            lot.setStockQuality(StockQuality.NORMAL);
        }

        ProductLot savedLot = productLotRepository.save(lot);

        // 3. 이력(Log) 저장
        saveInventoryLog(savedLot, dto.getQuantity(), AdjustmentReason.INBOUND, "최초 입고");

        return savedLot.getId();
    }

    /**
     * [본사] 전체 재고 현황 조회
     */
    // 화면에 재고 목록을 Table 형태로 보여줘야 하므로, 하나가 아닌 여러 개의 데이터 객체가 담긴 List 사용
    @Transactional(readOnly = true)
    public List<ProductLot> getAllInventory() {
        List<ProductLot> list = productLotRepository.findAll();
        System.out.println(">>> DB에서 가져온 데이터 개수: " + list.size()); // 이거 추가
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

    // 재고 목록 조회 (검색 + 페이징)
    @Transactional(readOnly = true)
    public Page<ProductLot> getStockList(Pageable pageable, String keyword) {
        return productLotRepository.searchStock(keyword, pageable);
    }

    // 재고 삭제
    public void deleteStocks(List<Long> lotIds) {
        for (Long lotId : lotIds) {
            // 1. 해당 재고의 이력(로그) 모두 삭제
            inventoryLogRepository.deleteByProductLotId(lotId);

            // 2. 재고(Lot) 삭제
            productLotRepository.deleteById(lotId);
        }
    }
}
