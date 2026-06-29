package org.example.bank_system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PayInstalmentRequest(
        @NotNull @Positive Long accountId
) {}
