package com.age.b2b.service;

import com.age.b2b.domain.Product;
import com.age.b2b.domain.ProductLot;
import com.age.b2b.domain.common.StockQuality;
import com.age.b2b.dto.ReceivingRequestDto;
import com.age.b2b.dto.ReceivingResponseDto;
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
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceivingService {

    private final ProductLotRepository productLotRepository;
    private final ProductRepository productRepository;

    // 입고 목록 조회
    public Page<ReceivingResponseDto> getReceivingList(String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt")); // 10개씩 조회

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

        // 2. Lot 번호 생성 (LOT-년월일-난수) -> 이게 입고번호 역할
        String lotNumber = generateLotNumber();



        // 3. 엔티티 생성 및 저장 (추가) 상품코드
        ProductLot lot = new ProductLot();
        lot.setProduct(product);
        lot.setProductCode(product.getProductCode());
        lot.setLotNumber(lotNumber);
        lot.setQuantity(dto.getQty());
        lot.setExpiryDate(dto.getExpireDate());
        lot.setInboundDate(java.time.LocalDate.now()); // 오늘 날짜
        lot.setStockQuality(StockQuality.NORMAL); // 기본 정상
        // lot.setWarehouseLocation("A-01"); // 기본 창고 위치 (필요시 추가)

        // 원산지 정보가 상품 정보와 다를 수 있지만, ProductLot에는 origin 필드가 없으므로
        // 필요하다면 Product 정보를 업데이트하거나 무시합니다. 여기서는 무시합니다.

        productLotRepository.save(lot);
    }

    private String generateLotNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(100, 999);
        return "LOT-" + date + "-" + random;
    }
}