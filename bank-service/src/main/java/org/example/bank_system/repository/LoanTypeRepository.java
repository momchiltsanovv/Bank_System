package org.example.bank_system.repository;

import org.example.bank_system.entity.LoanCategory;
import org.example.bank_system.entity.LoanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanTypeRepository extends JpaRepository<LoanType, Long> {
    Optional<LoanType> findByCategory(LoanCategory category);
}
