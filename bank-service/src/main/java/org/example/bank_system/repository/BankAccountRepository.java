package org.example.bank_system.repository;

import jakarta.persistence.LockModeType;
import org.example.bank_system.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByOwnerId(Long clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BankAccount> findLockedById(Long id);

    Optional<BankAccount> findByIban(String iban);
    boolean existsByIban(String iban);
}
