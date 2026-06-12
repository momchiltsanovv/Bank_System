package org.example.bank_system.dto.response;

import org.example.bank_system.entity.AccountStatus;

import java.math.BigDecimal;

public record BankAccountResponse(
        Long id,
        String iban,
        BigDecimal balance,
        AccountStatus status,
        Long clientId
) {}
