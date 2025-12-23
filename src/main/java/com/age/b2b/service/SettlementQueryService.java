package com.age.b2b.service;

import com.age.b2b.dto.AdminSettlementListDto;
import com.age.b2b.dto.SettlementListDto;
import com.age.b2b.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.age.b2b.domain.Client;
import com.age.b2b.repository.ClientRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementQueryService {

    private final SettlementRepository settlementRepository;
    private final ClientRepository clientRepository;

    /**
     * [고객사 전용] 정산관리 화면 목록 조회
     */
    public Page<SettlementListDto> getSettlementList(String username, int page, int size, String keyword) {
        Client client = clientRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : 10);

        // 특정 고객사의 ID로 필터링하여 조회
        return settlementRepository.findSettlementList(client.getClientId(), keyword, pageable);
    }

    /**
     * [본사 관리자 전용] 시스템 전체 정산현황 조회 (4개 테이블 조인)
     * 본사는 모든 고객사의 데이터를 봐야 하므로 client 조회가 필요 없습니다.
     */
    public Page<AdminSettlementListDto> getAdminSettlementList(int page, int size, String keyword) {

        // 페이징 설정
        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : 10);

        // 모든 고객사의 데이터를 가져오는 Repository 메서드 호출
        // (Settlement + Order + Product + Client 조인 쿼리)
        return settlementRepository.findAllSettlementList(keyword, pageable);
    }
}