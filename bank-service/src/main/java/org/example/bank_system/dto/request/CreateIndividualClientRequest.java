package org.example.bank_system.dto.request;

import jakarta.validation.constraints.*;

public record CreateIndividualClientRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Size(min = 10, max = 10) @Pattern(regexp = "\\d{10}") String egn
) {}
