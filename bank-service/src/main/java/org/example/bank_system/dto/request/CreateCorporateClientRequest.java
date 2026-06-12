package org.example.bank_system.dto.request;

import jakarta.validation.constraints.*;

public record CreateCorporateClientRequest(
        @NotBlank String companyName,
        @NotBlank @Size(min = 9, max = 13) @Pattern(regexp = "\\d{9,13}") String eik,
        @NotBlank String representativeFirstName,
        @NotBlank String representativeLastName
) {}
