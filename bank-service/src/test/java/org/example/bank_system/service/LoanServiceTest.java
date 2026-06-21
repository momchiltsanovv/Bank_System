package org.example.bank_system.service;

import org.example.bank_system.dto.request.GrantLoanRequest;
import org.example.bank_system.dto.response.LoanResponse;
import org.example.bank_system.entity.*;
import org.example.bank_system.exception.BusinessRuleException;
import org.example.bank_system.repository.LoanRepository;
import org.example.bank_system.repository.LoanTypeRepository;
import org.example.bank_system.repository.RepaymentInstalmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock LoanRepository loanRepo;
    @Mock LoanTypeRepository loanTypeRepo;
    @Mock RepaymentInstalmentRepository instalmentRepo;
    @Mock ClientService clientService;
    @InjectMocks LoanService loanService;

    private LoanType consumerType() {
        LoanType lt = new LoanType();
        lt.setCategory(LoanCategory.CONSUMER);
        lt.setAnnualInterestRate(new BigDecimal("0.085"));
        lt.setMaxAmount(new BigDecimal("50000"));
        lt.setMaxTermMonths(84);
        return lt;
    }

    private Loan savedLoan(LoanType loanType, IndividualClient client, BigDecimal amount, int term) {
        Loan loan = new Loan();
        loan.setLoanType(loanType);
        loan.setClient(client);
        loan.setAmount(amount);
        loan.setTermMonths(term);
        return loan;
    }

    @Test
    void grantLoan_consumer_success() {
        LoanType lt = consumerType();
        IndividualClient client = new IndividualClient();
        client.setFirstName("Ivan");

        when(loanTypeRepo.findByCategory(LoanCategory.CONSUMER)).thenReturn(Optional.of(lt));
        when(clientService.getClientEntityById(1L)).thenReturn(client);
        when(loanRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(instalmentRepo.countByLoanIdAndPaidTrue(any())).thenReturn(0L);

        LoanResponse resp = loanService.grantLoan(
                new GrantLoanRequest(1L, LoanCategory.CONSUMER, new BigDecimal("10000"), 12));

        assertThat(resp.amount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(resp.termMonths()).isEqualTo(12);
        assertThat(resp.loanCategory()).isEqualTo(LoanCategory.CONSUMER);
        assertThat(resp.repaymentPlan()).hasSize(12);
    }

    @Test
    void grantLoan_exceedsMaxAmount_throws() {
        when(loanTypeRepo.findByCategory(LoanCategory.CONSUMER)).thenReturn(Optional.of(consumerType()));

        assertThatThrownBy(() -> loanService.grantLoan(
                new GrantLoanRequest(1L, LoanCategory.CONSUMER, new BigDecimal("60000"), 12)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("50000");
    }

    @Test
    void grantLoan_exceedsMaxTerm_throws() {
        when(loanTypeRepo.findByCategory(LoanCategory.CONSUMER)).thenReturn(Optional.of(consumerType()));

        assertThatThrownBy(() -> loanService.grantLoan(
                new GrantLoanRequest(1L, LoanCategory.CONSUMER, new BigDecimal("10000"), 100)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("84");
    }

    @Test
    void payInstalment_alreadyPaid_throws() {
        RepaymentInstalment inst = new RepaymentInstalment();
        inst.setPaid(true);
        when(instalmentRepo.findByLoanIdAndMonthNumber(1L, 1)).thenReturn(Optional.of(inst));

        assertThatThrownBy(() -> loanService.payInstalment(1L, 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void getLoanStatus_partial() {
        Loan loan = new Loan();
        loan.setTermMonths(12);
        when(loanRepo.findById(1L)).thenReturn(Optional.of(loan));
        when(instalmentRepo.countByLoanIdAndPaidTrue(1L)).thenReturn(5L);

        var status = loanService.getLoanStatus(1L);

        assertThat(status.paidInstalments()).isEqualTo(5L);
        assertThat(status.totalInstalments()).isEqualTo(12L);
        assertThat(status.fullyPaid()).isFalse();
    }

    @Test
    void getLoanStatus_fullyPaid() {
        Loan loan = new Loan();
        loan.setTermMonths(12);
        when(loanRepo.findById(1L)).thenReturn(Optional.of(loan));
        when(instalmentRepo.countByLoanIdAndPaidTrue(1L)).thenReturn(12L);

        var status = loanService.getLoanStatus(1L);

        assertThat(status.fullyPaid()).isTrue();
        assertThat(status.paidInstalments()).isEqualTo(status.totalInstalments());
    }
}
