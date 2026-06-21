package org.example.bank_system.repository;

import org.example.bank_system.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByClientId(Long clientId);
    boolean existsByLoanTypeIdAndAmountGreaterThan(Long loanTypeId, BigDecimal maxAmount);
    boolean existsByLoanTypeIdAndTermMonthsGreaterThan(Long loanTypeId, Integer maxTermMonths);
}
