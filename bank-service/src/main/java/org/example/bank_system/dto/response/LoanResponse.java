package org.example.bank_system.dto.response;

import org.example.bank_system.entity.LoanCategory;

import java.math.BigDecimal;
import java.util.List;

public record LoanResponse(
        Long id,
        Long clientId,
        LoanCategory loanCategory,
        BigDecimal amount,
        Integer termMonths,
        long paidInstalments,
        long totalInstalments,
        List<RepaymentInstalmentResponse> repaymentPlan
) {}
