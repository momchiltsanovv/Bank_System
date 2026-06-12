package org.example.bank_system.dto.response;

import org.example.bank_system.entity.LoanCategory;

import java.math.BigDecimal;

public record LoanTypeResponse(
        Long id,
        LoanCategory category,
        BigDecimal annualInterestRate,
        BigDecimal maxAmount,
        Integer maxTermMonths
) {}
