package org.example.bank_system.repository;

import org.example.bank_system.entity.IndividualClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IndividualClientRepository extends JpaRepository<IndividualClient, Long> {
    Optional<IndividualClient> findByEgn(String egn);
    boolean existsByEgn(String egn);
}
