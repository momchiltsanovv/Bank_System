package org.example.bank_system.dto.response;

public record CorporateClientResponse(
        Long id,
        String companyName,
        String eik,
        String representativeFirstName,
        String representativeLastName
) {}
