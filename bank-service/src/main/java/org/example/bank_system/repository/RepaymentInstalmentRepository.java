package org.example.bank_system.repository;

import org.example.bank_system.entity.RepaymentInstalment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepaymentInstalmentRepository extends JpaRepository<RepaymentInstalment, Long> {
    List<RepaymentInstalment> findByLoanIdOrderByMonthNumberAsc(Long loanId);
    Optional<RepaymentInstalment> findByLoanIdAndMonthNumber(Long loanId, Integer monthNumber);
    long countByLoanIdAndPaidTrue(Long loanId);
}
