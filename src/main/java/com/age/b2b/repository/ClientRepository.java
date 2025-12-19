package com.age.b2b.repository;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.common.ClientStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByUsername(String username);       // 아이디 중복 확인
    boolean existsByBusinessNumber(String businessNumber); // 사업자번호 중복 확인
    boolean existsByEmail(String email);             // 이메일 중복 확인
    boolean existsByPhone(String phone);             // 연락처 중복 확인

    Optional<Client> findByUsername(String username);

    // [본사] 승인 대기중인 고객사 목록 조회
    List<Client> findByApprovalStatus(ClientStatus status);
}
