package org.example.bank_system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bank_system.dto.request.GrantLoanRequest;
import org.example.bank_system.dto.request.PayInstalmentRequest;
import org.example.bank_system.dto.request.UpdateLoanTypeRequest;
import org.example.bank_system.dto.response.LoanResponse;
import org.example.bank_system.dto.response.LoanTypeResponse;
import org.example.bank_system.dto.response.RepaymentInstalmentResponse;
import org.example.bank_system.service.LoanService;
import org.example.bank_system.service.LoanService.LoanStatusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse grantLoan(@Valid @RequestBody GrantLoanRequest req) {
        return loanService.grantLoan(req);
    }

    @GetMapping("/{id}")
    public LoanResponse getLoan(@PathVariable("id") Long id) {
        return loanService.getLoan(id);
    }

    @GetMapping("/client/{clientId}")
    public List<LoanResponse> getLoansByClient(@PathVariable("clientId") Long clientId) {
        return loanService.getLoansByClient(clientId);
    }

    @GetMapping("/{id}/repayment-plan")
    public List<RepaymentInstalmentResponse> getRepaymentPlan(@PathVariable("id") Long id) {
        return loanService.getLoan(id).repaymentPlan();
    }

    @PatchMapping("/{loanId}/instalments/{monthNumber}/pay")
    public RepaymentInstalmentResponse payInstalment(@PathVariable("loanId") Long loanId,
                                                     @PathVariable("monthNumber") Integer monthNumber,
                                                     @Valid @RequestBody PayInstalmentRequest req) {
        return loanService.payInstalment(loanId, monthNumber, req.accountId());
    }

    @GetMapping("/{id}/status")
    public LoanStatusResponse getStatus(@PathVariable("id") Long id) {
        return loanService.getLoanStatus(id);
    }

    @GetMapping("/types")
    public List<LoanTypeResponse> getLoanTypes() {
        return loanService.getAllLoanTypes();
    }

    @PatchMapping("/types/{id}")
    public LoanTypeResponse updateLoanType(@PathVariable("id") Long id,
                                           @Valid @RequestBody UpdateLoanTypeRequest req) {
        return loanService.updateLoanType(id, req);
    }
}
