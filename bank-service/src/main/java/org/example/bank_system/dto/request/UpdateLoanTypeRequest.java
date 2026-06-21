package org.example.bank_system.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateLoanTypeRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal annualInterestRate,
        @NotNull @Positive BigDecimal maxAmount,
        @NotNull @Positive Integer maxTermMonths
) {}
