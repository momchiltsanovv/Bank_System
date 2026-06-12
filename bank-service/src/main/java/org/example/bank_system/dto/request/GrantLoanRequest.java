package org.example.bank_system.dto.request;

import jakarta.validation.constraints.*;
import org.example.bank_system.entity.LoanCategory;

import java.math.BigDecimal;

public record GrantLoanRequest(
        @NotNull Long clientId,
        @NotNull LoanCategory loanCategory,
        @NotNull @Positive BigDecimal amount,
        @NotNull @Positive Integer termMonths
) {}
