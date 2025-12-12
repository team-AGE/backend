package com.age.b2b.repository;

import com.age.b2b.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByUsername(String username);
    boolean existsByBusinessNumber(String businessNumber);
    boolean existsByEmail(String email);

    Optional<Client> findByUsername(String username);
}
