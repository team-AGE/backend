package com.age.b2b.service;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.common.ClientStatus;
import com.age.b2b.dto.ClientJoinDto;
import com.age.b2b.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientService {
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long join(ClientJoinDto dto) {
        // 1. 중복 검증
        if (clientRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        if (clientRepository.existsByBusinessNumber(dto.getBusinessNumber())) {
            throw new IllegalArgumentException("이미 등록된 사업자 번호입니다.");
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // 3. Entity 생성 (Builder 혹은 생성자 사용)
        // Entity에 Set
        Client client = new Client();
        client.setUsername(dto.getUsername());
        client.setPassword(encodedPassword); // 암호화된 비밀번호 저장
        client.setClientCategory(dto.getClientCategory());
        client.setBusinessName(dto.getBusinessName());
        client.setBusinessNumber(dto.getBusinessNumber());
        client.setOwnerName(dto.getOwnerName());
        client.setPhone(dto.getPhone());
        client.setEmail(dto.getEmail());
        client.setAddress(dto.getAddress());

        // 4. 초기 상태 설정 (승인 대기)
        client.setApprovalStatus(ClientStatus.WAITING);

        // 5. 저장
        return clientRepository.save(client).getClientId();
    }
}
