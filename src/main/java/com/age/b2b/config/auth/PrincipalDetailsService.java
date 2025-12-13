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
            // ★ 수정: 상태 체크 로직 삭제! 그냥 객체만 리턴하면 됨.
            // (체크는 PrincipalDetails.isEnabled()에서 자동으로 함)
            return new PrincipalDetails(clientEntity.get());
        }

        throw new UsernameNotFoundException("존재하지 않는 아이디 또는 비밀번호입니다.");
    }
}