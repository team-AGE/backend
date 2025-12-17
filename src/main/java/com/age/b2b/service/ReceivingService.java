package com.age.b2b.service;

import com.age.b2b.domain.ProductLot;
import com.age.b2b.dto.ReceivingResponseDto;
import com.age.b2b.repository.ProductLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceivingService {

    private final ProductLotRepository productLotRepository;

    // 입고 목록 조회
    public Page<ReceivingResponseDto> getReceivingList(String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 10); // 10개씩 조회

        Page<ProductLot> lots;
        if (keyword != null && !keyword.isBlank()) {
            lots = productLotRepository.searchByKeyword(keyword, pageable);
        } else {
            lots = productLotRepository.findAllWithProduct(pageable);
        }

        // 엔티티 -> DTO 변환
        return lots.map(ReceivingResponseDto::from);
    }
}