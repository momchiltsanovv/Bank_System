package org.example.bank_system.dto.response;

public record IndividualClientResponse(
        Long id,
        String firstName,
        String lastName,
        String egn
) {}
