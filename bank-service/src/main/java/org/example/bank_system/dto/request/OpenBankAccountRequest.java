package org.example.bank_system.dto.request;

import jakarta.validation.constraints.*;

public record OpenBankAccountRequest(
        @NotNull Long clientId,
        @NotBlank @Pattern(regexp = "[A-Z]{2}\\d{2}[A-Z0-9]{1,30}", message = "Invalid IBAN format") String iban
) {}
