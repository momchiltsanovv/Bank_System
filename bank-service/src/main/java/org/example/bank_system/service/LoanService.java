package org.example.bank_system.service;

import lombok.RequiredArgsConstructor;
import org.example.bank_system.dto.request.GrantLoanRequest;
import org.example.bank_system.dto.request.UpdateLoanTypeRequest;
import org.example.bank_system.dto.response.LoanResponse;
import org.example.bank_system.dto.response.LoanTypeResponse;
import org.example.bank_system.dto.response.RepaymentInstalmentResponse;
import org.example.bank_system.entity.*;
import org.example.bank_system.exception.BusinessRuleException;
import org.example.bank_system.exception.ResourceNotFoundException;
import org.example.bank_system.repository.LoanRepository;
import org.example.bank_system.repository.LoanTypeRepository;
import org.example.bank_system.repository.RepaymentInstalmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepo;
    private final LoanTypeRepository loanTypeRepo;
    private final RepaymentInstalmentRepository instalmentRepo;
    private final ClientService clientService;

    @Transactional
    public LoanResponse grantLoan(GrantLoanRequest req) {
        LoanType loanType = loanTypeRepo.findByCategory(req.loanCategory())
                .orElseThrow(() -> new ResourceNotFoundException("Loan type not found: " + req.loanCategory()));

        if (req.amount().compareTo(loanType.getMaxAmount()) > 0) {
            throw new BusinessRuleException("Requested amount " + req.amount()
                    + " exceeds maximum allowed " + loanType.getMaxAmount()
                    + " for " + req.loanCategory() + " loans");
        }
        if (req.termMonths() > loanType.getMaxTermMonths()) {
            throw new BusinessRuleException("Requested term " + req.termMonths()
                    + " months exceeds maximum allowed " + loanType.getMaxTermMonths()
                    + " for " + req.loanCategory() + " loans");
        }

        Client client = clientService.getClientEntityById(req.clientId());

        Loan loan = new Loan();
        loan.setClient(client);
        loan.setLoanType(loanType);
        loan.setAmount(req.amount());
        loan.setTermMonths(req.termMonths());

        List<RepaymentInstalment> plan = buildAnnuityPlan(loan, loanType.getAnnualInterestRate(), req.amount(), req.termMonths());
        loan.setRepaymentPlan(plan);

        loan = loanRepo.save(loan);
        return toResponse(loan);
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoan(Long loanId) {
        return toResponse(findLoan(loanId));
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getLoansByClient(Long clientId) {
        return loanRepo.findByClientId(clientId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public RepaymentInstalmentResponse payInstalment(Long loanId, Integer monthNumber) {
        RepaymentInstalment instalment = instalmentRepo.findByLoanIdAndMonthNumber(loanId, monthNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instalment not found for loan " + loanId + " month " + monthNumber));
        if (instalment.isPaid()) {
            throw new BusinessRuleException("Instalment " + monthNumber + " is already paid");
        }
        instalment.setPaid(true);
        return toInstalmentResponse(instalmentRepo.save(instalment));
    }

    @Transactional(readOnly = true)
    public LoanStatusResponse getLoanStatus(Long loanId) {
        Loan loan = findLoan(loanId);
        long paid = instalmentRepo.countByLoanIdAndPaidTrue(loanId);
        long total = loan.getTermMonths();
        return new LoanStatusResponse(loanId, paid, total, paid == total);
    }

    @Transactional(readOnly = true)
    public List<LoanTypeResponse> getAllLoanTypes() {
        return loanTypeRepo.findAll().stream()
                .map(lt -> new LoanTypeResponse(lt.getId(), lt.getCategory(),
                        lt.getAnnualInterestRate(), lt.getMaxAmount(), lt.getMaxTermMonths()))
                .toList();
    }

    @Transactional
    public LoanTypeResponse updateLoanType(Long id, UpdateLoanTypeRequest req) {
        LoanType lt = loanTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan type not found: " + id));
        if (loanRepo.existsByLoanTypeIdAndAmountGreaterThan(id, req.maxAmount())) {
            throw new BusinessRuleException(
                    "Cannot reduce maxAmount: existing loans exceed " + req.maxAmount());
        }
        if (loanRepo.existsByLoanTypeIdAndTermMonthsGreaterThan(id, req.maxTermMonths())) {
            throw new BusinessRuleException(
                    "Cannot reduce maxTermMonths: existing loans exceed " + req.maxTermMonths());
        }
        lt.setAnnualInterestRate(req.annualInterestRate());
        lt.setMaxAmount(req.maxAmount());
        lt.setMaxTermMonths(req.maxTermMonths());
        lt = loanTypeRepo.save(lt);
        return new LoanTypeResponse(lt.getId(), lt.getCategory(),
                lt.getAnnualInterestRate(), lt.getMaxAmount(), lt.getMaxTermMonths());
    }

    // --- Annuity repayment plan ---

    /**
     * Annuity formula: M = P * r / (1 - (1+r)^-n)
     * where r = monthly rate, n = term in months, P = principal
     */
    private List<RepaymentInstalment> buildAnnuityPlan(Loan loan, BigDecimal annualRate,
                                                        BigDecimal principal, int termMonths) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal monthlyPayment;
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            monthlyPayment = principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        } else {
            // (1 + r)^n
            BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
            BigDecimal factor = onePlusR.pow(termMonths, new MathContext(15, RoundingMode.HALF_UP));
            // M = P * r * (1+r)^n / ((1+r)^n - 1)
            BigDecimal numerator = principal.multiply(monthlyRate).multiply(factor);
            BigDecimal denominator = factor.subtract(BigDecimal.ONE);
            monthlyPayment = numerator.divide(denominator, 2, RoundingMode.HALF_UP);
        }

        List<RepaymentInstalment> plan = new ArrayList<>();
        BigDecimal remaining = principal;

        for (int month = 1; month <= termMonths; month++) {
            BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart;
            if (month == termMonths) {
                // last instalment: clear remaining; guard against negative drift
                if (remaining.signum() < 0) {
                    throw new BusinessRuleException(
                            "Annuity rounding error: remaining balance went negative at month " + month);
                }
                principalPart = remaining;
            } else {
                principalPart = monthlyPayment.subtract(interest);
                if (principalPart.signum() <= 0) {
                    throw new BusinessRuleException(
                            "Annuity calculation error: non-positive principal part at month " + month);
                }
            }
            remaining = remaining.subtract(principalPart).setScale(2, RoundingMode.HALF_UP);

            RepaymentInstalment instalment = new RepaymentInstalment();
            instalment.setLoan(loan);
            instalment.setMonthNumber(month);
            instalment.setTotalPayment(principalPart.add(interest));
            instalment.setPrincipalPart(principalPart);
            instalment.setInterestPart(interest);
            instalment.setRemainingBalance(remaining);
            plan.add(instalment);
        }
        return plan;
    }

    private Loan findLoan(Long loanId) {
        return loanRepo.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
    }

    private LoanResponse toResponse(Loan loan) {
        long paid = instalmentRepo.countByLoanIdAndPaidTrue(loan.getId());
        List<RepaymentInstalmentResponse> plan = loan.getRepaymentPlan().stream()
                .map(this::toInstalmentResponse).toList();
        return new LoanResponse(loan.getId(), loan.getClient().getId(),
                loan.getLoanType().getCategory(), loan.getAmount(), loan.getTermMonths(),
                paid, loan.getTermMonths(), plan);
    }

    private RepaymentInstalmentResponse toInstalmentResponse(RepaymentInstalment i) {
        return new RepaymentInstalmentResponse(i.getId(), i.getMonthNumber(), i.getTotalPayment(),
                i.getPrincipalPart(), i.getInterestPart(), i.getRemainingBalance(), i.isPaid());
    }

    public record LoanStatusResponse(Long loanId, long paidInstalments, long totalInstalments, boolean fullyPaid) {}
}
