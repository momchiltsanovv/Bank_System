package org.example.bank_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Represents the bank's loan product (e.g. Consumer at 8%, max 50 000 BGN, 84 months).
 * Seeded at startup; not created by end-users.
 */
@Getter
@Setter
@Entity
@Table(name = "loan_types")
public class LoanType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private LoanCategory category;

    /** Annual interest rate, e.g. 0.08 for 8% */
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal annualInterestRate;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer maxTermMonths;
}
