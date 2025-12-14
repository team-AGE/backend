package com.age.b2b.service;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.common.ClientStatus;
import com.age.b2b.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminService {

    private final ClientRepository clientRepository;

    /**
     * [본사] 승인 대기중인 고객사 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Client> getWaitingClients() {
        return clientRepository.findByApprovalStatus(ClientStatus.WAITING);
    }

    /**
     * [본사] 고객사 가입 승인 처리
     */
    public void approveClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고객사입니다."));

        if (client.getApprovalStatus() != ClientStatus.WAITING) {
            throw new IllegalStateException("대기 상태의 회원만 승인할 수 있습니다.");
        }

        client.setApprovalStatus(ClientStatus.APPROVED);
    }

    /**
     * [본사] 고객사 가입 거절 처리
     */
    public void rejectClient(Long clientId, String reason) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고객사입니다."));

        client.setApprovalStatus(ClientStatus.REJECTED);
        client.setNote(reason); // 거절 사유 기록
    }
}