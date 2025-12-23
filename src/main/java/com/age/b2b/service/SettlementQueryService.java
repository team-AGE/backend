package com.age.b2b.service;

import com.age.b2b.dto.SettlementListDto;
import com.age.b2b.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 전용이므로 성능 최적화를 위해 readOnly 적용
public class SettlementQueryService {

    private final SettlementRepository settlementRepository;

    /**
     * 정산관리 화면 목록 조회 (페이징 처리)
     *
     * @param page    요청 페이지 번호 (0부터 시작)
     * @param size    한 페이지당 보여줄 데이터 개수
     * @param keyword
     * @return DTO로 변환된 페이징 객체
     */
    public Page<SettlementListDto> getSettlementList(int page, int size, String keyword) {

        // 페이지 번호가 음수가 되지 않도록 방어 로직 추가
        int safePage = Math.max(page, 0);
        // 한 페이지당 사이즈가 0 이하일 경우 기본값 10 설정
        int safeSize = size > 0 ? size : 10;

        // 페이징 정보 생성
        Pageable pageable = PageRequest.of(safePage, safeSize);

        // Repository의 커스텀 @Query 호출
        return settlementRepository.findSettlementList(keyword,pageable);
    }
}