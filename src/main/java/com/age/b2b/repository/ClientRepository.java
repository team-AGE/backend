package com.age.b2b.repository;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.common.ClientStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByUsername(String username);
    boolean existsByBusinessNumber(String businessNumber);
    boolean existsByEmail(String email);

    Optional<Client> findByUsername(String username);

    // [본사] 승인 대기중인 고객사 목록 조회
    List<Client> findByApprovalStatus(ClientStatus status);

}
