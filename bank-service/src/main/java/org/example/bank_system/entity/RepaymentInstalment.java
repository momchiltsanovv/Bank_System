package org.example.bank_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "repayment_instalments")
public class RepaymentInstalment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer monthNumber;

    /** Fixed annuity payment = principal part + interest part */
    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPayment;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalPart;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal interestPart;

    /** Remaining principal balance after this instalment is paid */
    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalance;

    @Column(nullable = false)
    private boolean paid = false;
}
