package org.example.bank_system.repository;

import org.example.bank_system.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByOwnerId(Long clientId);
    Optional<BankAccount> findByIban(String iban);
    boolean existsByIban(String iban);
}
