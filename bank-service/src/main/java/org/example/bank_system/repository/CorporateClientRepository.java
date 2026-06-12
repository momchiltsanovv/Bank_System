package org.example.bank_system.repository;

import org.example.bank_system.entity.CorporateClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CorporateClientRepository extends JpaRepository<CorporateClient, Long> {
    Optional<CorporateClient> findByEik(String eik);
    boolean existsByEik(String eik);
}
