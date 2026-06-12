package org.example.bank_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "corporate_clients")
@DiscriminatorValue("CORPORATE")
public class CorporateClient extends Client {

    @NotBlank
    @Column(nullable = false)
    private String companyName;

    @NotBlank
    @Size(min = 9, max = 13, message = "EIK must be between 9 and 13 digits")
    @Pattern(regexp = "\\d{9,13}", message = "EIK must contain only digits")
    @Column(nullable = false, unique = true, length = 13)
    private String eik;

    @NotBlank
    @Column(nullable = false)
    private String representativeFirstName;

    @NotBlank
    @Column(nullable = false)
    private String representativeLastName;
}
