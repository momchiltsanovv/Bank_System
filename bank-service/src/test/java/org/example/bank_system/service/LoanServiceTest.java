package org.example.bank_system.service;

import org.example.bank_system.dto.request.GrantLoanRequest;
import org.example.bank_system.dto.response.LoanResponse;
import org.example.bank_system.entity.*;
import org.example.bank_system.exception.BusinessRuleException;
import org.example.bank_system.repository.LoanRepository;
import org.example.bank_system.repository.LoanTypeRepository;
import org.example.bank_system.repository.RepaymentInstalmentRepository;
import org.example.bank_system.repository.BankAccountRepository;
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
    @Mock BankAccountRepository accountRepo;
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

        assertThatThrownBy(() -> loanService.payInstalment(1L, 1, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void payInstalment_whenPreviousInstalmentsAreUnpaid_throws() {
        RepaymentInstalment inst = new RepaymentInstalment();
        inst.setMonthNumber(3);
        inst.setPaid(false);
        when(instalmentRepo.findByLoanIdAndMonthNumber(1L, 3)).thenReturn(Optional.of(inst));
        when(instalmentRepo.existsByLoanIdAndMonthNumberLessThanAndPaidFalse(1L, 3)).thenReturn(true);

        assertThatThrownBy(() -> loanService.payInstalment(1L, 3, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Previous instalments");
    }

    @Test
    void payInstalment_whenBalanceIsInsufficient_throws() {
        RepaymentInstalment inst = instalmentForLoan(new BigDecimal("100.00"));
        BankAccount account = accountForLoan(inst);
        account.setBalance(new BigDecimal("99.99"));

        when(instalmentRepo.findByLoanIdAndMonthNumber(1L, 1)).thenReturn(Optional.of(inst));
        when(accountRepo.findLockedById(10L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> loanService.payInstalment(1L, 1, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    void payInstalment_whenAccountBelongsToDifferentClient_throws() {
        RepaymentInstalment inst = instalmentForLoan(new BigDecimal("100.00"));
        BankAccount account = accountForLoan(inst);
        IndividualClient otherClient = new IndividualClient();
        otherClient.setId(2L);
        account.setOwner(otherClient);

        when(instalmentRepo.findByLoanIdAndMonthNumber(1L, 1)).thenReturn(Optional.of(inst));
        when(accountRepo.findLockedById(10L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> loanService.payInstalment(1L, 1, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void payInstalment_whenAccountIsClosed_throws() {
        RepaymentInstalment inst = instalmentForLoan(new BigDecimal("100.00"));
        BankAccount account = accountForLoan(inst);
        account.setStatus(AccountStatus.CLOSED);

        when(instalmentRepo.findByLoanIdAndMonthNumber(1L, 1)).thenReturn(Optional.of(inst));
        when(accountRepo.findLockedById(10L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> loanService.payInstalment(1L, 1, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("closed account");
    }

    @Test
    void payInstalment_withValidAccount_deductsBalanceAndMarksPaid() {
        RepaymentInstalment inst = instalmentForLoan(new BigDecimal("100.00"));
        BankAccount account = accountForLoan(inst);
        account.setBalance(new BigDecimal("250.00"));

        when(instalmentRepo.findByLoanIdAndMonthNumber(1L, 1)).thenReturn(Optional.of(inst));
        when(accountRepo.findLockedById(10L)).thenReturn(Optional.of(account));
        when(instalmentRepo.save(inst)).thenReturn(inst);

        var response = loanService.payInstalment(1L, 1, 10L);

        assertThat(response.paid()).isTrue();
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        verify(accountRepo).save(account);
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

    private RepaymentInstalment instalmentForLoan(BigDecimal totalPayment) {
        IndividualClient client = new IndividualClient();
        client.setId(1L);
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setClient(client);

        RepaymentInstalment inst = new RepaymentInstalment();
        inst.setLoan(loan);
        inst.setMonthNumber(1);
        inst.setTotalPayment(totalPayment);
        inst.setPrincipalPart(totalPayment);
        inst.setInterestPart(BigDecimal.ZERO);
        inst.setRemainingBalance(BigDecimal.ZERO);
        inst.setPaid(false);
        return inst;
    }

    private BankAccount accountForLoan(RepaymentInstalment inst) {
        BankAccount account = new BankAccount();
        account.setId(10L);
        account.setOwner(inst.getLoan().getClient());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(new BigDecimal("1000.00"));
        return account;
    }
}
