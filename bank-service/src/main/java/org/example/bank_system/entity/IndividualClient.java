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
@Table(name = "individual_clients")
@DiscriminatorValue("INDIVIDUAL")
public class IndividualClient extends Client {

    @NotBlank
    @Column(nullable = false)
    private String firstName;

    @NotBlank
    @Column(nullable = false)
    private String lastName;

    @NotBlank
    @Size(min = 10, max = 10, message = "EGN must be exactly 10 digits")
    @Pattern(regexp = "\\d{10}", message = "EGN must contain only digits")
    @Column(nullable = false, unique = true, length = 10)
    private String egn;
}
