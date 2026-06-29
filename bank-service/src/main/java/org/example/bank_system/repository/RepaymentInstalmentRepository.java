package org.example.bank_system.repository;

import jakarta.persistence.LockModeType;
import org.example.bank_system.entity.RepaymentInstalment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface RepaymentInstalmentRepository extends JpaRepository<RepaymentInstalment, Long> {
    List<RepaymentInstalment> findByLoanIdOrderByMonthNumberAsc(Long loanId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RepaymentInstalment> findByLoanIdAndMonthNumber(Long loanId, Integer monthNumber);

    boolean existsByLoanIdAndMonthNumberLessThanAndPaidFalse(Long loanId, Integer monthNumber);

    long countByLoanIdAndPaidTrue(Long loanId);
}
