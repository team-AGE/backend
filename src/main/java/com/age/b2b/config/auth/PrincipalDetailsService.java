package com.age.b2b.config.auth;

import com.age.b2b.domain.Admin;
import com.age.b2b.domain.Client;
import com.age.b2b.domain.common.ClientStatus;
import com.age.b2b.repository.AdminRepository;
import com.age.b2b.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {
    private final AdminRepository adminRepository;
    private final ClientRepository clientRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. 관리자 조회
        Optional<Admin> adminEntity = adminRepository.findByUsername(username);
        if (adminEntity.isPresent()) {
            return new PrincipalDetails(adminEntity.get());
        }

        // 2. 고객사 조회
        Optional<Client> clientEntity = clientRepository.findByUsername(username);
        if (clientEntity.isPresent()) {
            Client client = clientEntity.get();

            // 3. 상태별 예외 분리 (메시지 다르게 설정)
            if (client.getApprovalStatus() == ClientStatus.WAITING) {
                // WAITING -> DisabledException (비활성화 상태)
                throw new DisabledException("아직 승인 대기 중인 계정입니다. 관리자 승인을 기다려주세요.");
            }
            else if (client.getApprovalStatus() == ClientStatus.REJECTED) {
                // REJECTED -> LockedException (잠긴 상태)
                throw new LockedException("가입이 거절된 계정입니다. 관리자에게 문의하세요.");
            }

            // APPROVED 상태일 때만 로그인 진행
            return new PrincipalDetails(client);
        }

        throw new UsernameNotFoundException("존재하지 않는 아이디 또는 비밀번호입니다.");
    }
}
