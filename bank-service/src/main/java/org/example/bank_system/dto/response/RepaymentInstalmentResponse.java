package org.example.bank_system.dto.response;

import java.math.BigDecimal;

public record RepaymentInstalmentResponse(
        Long id,
        Integer monthNumber,
        BigDecimal totalPayment,
        BigDecimal principalPart,
        BigDecimal interestPart,
        BigDecimal remainingBalance,
        boolean paid
) {}
