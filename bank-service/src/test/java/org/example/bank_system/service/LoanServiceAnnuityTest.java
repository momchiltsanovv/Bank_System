package org.example.bank_system.service;

import org.example.bank_system.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class LoanServiceAnnuityTest {

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        // Construct with nulls — we only test the private buildAnnuityPlan method via reflection
        loanService = new LoanService(null, null, null, null);
    }

    @Test
    void annuityPaymentsAreEqual() throws Exception {
        List<RepaymentInstalment> plan = invokeBuildPlan(
                new BigDecimal("0.08"), new BigDecimal("10000"), 12);

        BigDecimal first = plan.get(0).getTotalPayment();
        for (int i = 0; i < plan.size() - 1; i++) {
            assertThat(plan.get(i).getTotalPayment())
                    .as("Month %d payment should equal month 1", i + 1)
                    .isEqualByComparingTo(first);
        }
    }

    @Test
    void remainingBalanceReachesZeroAfterLastInstalment() throws Exception {
        List<RepaymentInstalment> plan = invokeBuildPlan(
                new BigDecimal("0.08"), new BigDecimal("10000"), 24);

        RepaymentInstalment last = plan.get(plan.size() - 1);
        assertThat(last.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void earlyInstalmentHasHigherInterestThanLate() throws Exception {
        List<RepaymentInstalment> plan = invokeBuildPlan(
                new BigDecimal("0.08"), new BigDecimal("50000"), 60);

        BigDecimal firstInterest = plan.get(0).getInterestPart();
        BigDecimal lastInterest = plan.get(plan.size() - 1).getInterestPart();
        assertThat(firstInterest).isGreaterThan(lastInterest);
    }

    @Test
    void earlyInstalmentHasLowerPrincipalThanLate() throws Exception {
        List<RepaymentInstalment> plan = invokeBuildPlan(
                new BigDecimal("0.08"), new BigDecimal("50000"), 60);

        BigDecimal firstPrincipal = plan.get(0).getPrincipalPart();
        BigDecimal lastPrincipal = plan.get(plan.size() - 1).getPrincipalPart();
        assertThat(firstPrincipal).isLessThan(lastPrincipal);
    }

    @Test
    void planHasCorrectNumberOfInstalments() throws Exception {
        List<RepaymentInstalment> plan = invokeBuildPlan(
                new BigDecimal("0.085"), new BigDecimal("20000"), 36);
        assertThat(plan).hasSize(36);
    }

    @SuppressWarnings("unchecked")
    private List<RepaymentInstalment> invokeBuildPlan(BigDecimal annualRate, BigDecimal principal,
                                                       int termMonths) throws Exception {
        Loan loan = new Loan();
        Method m = LoanService.class.getDeclaredMethod(
                "buildAnnuityPlan", Loan.class, BigDecimal.class, BigDecimal.class, int.class);
        m.setAccessible(true);
        return (List<RepaymentInstalment>) m.invoke(loanService, loan, annualRate, principal, termMonths);
    }
}
