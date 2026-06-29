package org.example.bank_system.config;

import lombok.RequiredArgsConstructor;
import org.example.bank_system.dto.request.CreateCorporateClientRequest;
import org.example.bank_system.dto.request.CreateIndividualClientRequest;
import org.example.bank_system.dto.request.GrantLoanRequest;
import org.example.bank_system.dto.request.OpenBankAccountRequest;
import org.example.bank_system.dto.response.BankAccountResponse;
import org.example.bank_system.dto.response.CorporateClientResponse;
import org.example.bank_system.dto.response.IndividualClientResponse;
import org.example.bank_system.entity.AccountStatus;
import org.example.bank_system.entity.BankAccount;
import org.example.bank_system.entity.LoanCategory;
import org.example.bank_system.entity.LoanType;
import org.example.bank_system.repository.BankAccountRepository;
import org.example.bank_system.repository.CorporateClientRepository;
import org.example.bank_system.repository.IndividualClientRepository;
import org.example.bank_system.repository.LoanTypeRepository;
import org.example.bank_system.service.BankAccountService;
import org.example.bank_system.service.ClientService;
import org.example.bank_system.service.LoanService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bank.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseSeeder implements ApplicationRunner {

    private static final String INDIVIDUAL_EGN = "8501010001";
    private static final String SECOND_INDIVIDUAL_EGN = "9203050002";
    private static final String CORPORATE_EIK = "123456789";

    private final LoanTypeRepository loanTypeRepo;
    private final BankAccountRepository accountRepo;
    private final IndividualClientRepository individualRepo;
    private final CorporateClientRepository corporateRepo;
    private final ClientService clientService;
    private final BankAccountService accountService;
    private final LoanService loanService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedLoanTypes();

        IndividualClientResponse maria = ensureIndividualClient(
                "Maria", "Petrova", INDIVIDUAL_EGN);
        IndividualClientResponse georgi = ensureIndividualClient(
                "Georgi", "Dimitrov", SECOND_INDIVIDUAL_EGN);
        CorporateClientResponse techno = ensureCorporateClient(
                "TechnoLogica Ltd", CORPORATE_EIK, "Elena", "Nikolova");

        ensureAccount(maria.id(), "BG80BNBG96611020345678", false);
        ensureAccount(maria.id(), "BG18BNBG96611020345679", true);
        ensureAccount(georgi.id(), "BG42BNBG96611020345680", false);
        ensureAccount(techno.id(), "BG64BNBG96611020345681", false);

        ensureLoan(maria.id(), LoanCategory.CONSUMER, BigDecimal.valueOf(10000), 12);
        ensureLoan(georgi.id(), LoanCategory.CONSUMER, BigDecimal.valueOf(25000), 36);
        ensureLoan(techno.id(), LoanCategory.MORTGAGE, BigDecimal.valueOf(150000), 240);
    }

    private void seedLoanTypes() {
        ensureLoanType(LoanCategory.CONSUMER, BigDecimal.valueOf(0.0850), BigDecimal.valueOf(50000), 84);
        ensureLoanType(LoanCategory.MORTGAGE, BigDecimal.valueOf(0.0450), BigDecimal.valueOf(500000), 360);
    }

    private void ensureLoanType(LoanCategory category, BigDecimal annualRate,
                                BigDecimal maxAmount, int maxTermMonths) {
        loanTypeRepo.findByCategory(category).orElseGet(() -> {
            LoanType loanType = new LoanType();
            loanType.setCategory(category);
            loanType.setAnnualInterestRate(annualRate);
            loanType.setMaxAmount(maxAmount);
            loanType.setMaxTermMonths(maxTermMonths);
            return loanTypeRepo.save(loanType);
        });
    }

    private IndividualClientResponse ensureIndividualClient(String firstName, String lastName, String egn) {
        return individualRepo.findByEgn(egn)
                .map(client -> new IndividualClientResponse(
                        client.getId(), client.getFirstName(), client.getLastName(), client.getEgn()))
                .orElseGet(() -> clientService.createIndividual(
                        new CreateIndividualClientRequest(firstName, lastName, egn)));
    }

    private CorporateClientResponse ensureCorporateClient(String companyName, String eik,
                                                          String representativeFirstName,
                                                          String representativeLastName) {
        return corporateRepo.findByEik(eik)
                .map(client -> new CorporateClientResponse(
                        client.getId(), client.getCompanyName(), client.getEik(),
                        client.getRepresentativeFirstName(), client.getRepresentativeLastName()))
                .orElseGet(() -> clientService.createCorporate(new CreateCorporateClientRequest(
                        companyName, eik, representativeFirstName, representativeLastName)));
    }

    private void ensureAccount(Long clientId, String iban, boolean closeAfterCreation) {
        BankAccount account = accountRepo.findByIban(iban)
                .orElseGet(() -> {
                    BankAccountResponse response = accountService.openAccount(
                            new OpenBankAccountRequest(clientId, iban));
                    return accountRepo.findById(response.id()).orElseThrow();
                });

        if (closeAfterCreation && account.getStatus() == AccountStatus.ACTIVE) {
            accountService.closeAccount(account.getId());
        }
    }

    private void ensureLoan(Long clientId, LoanCategory category, BigDecimal amount, int termMonths) {
        boolean exists = loanService.getLoansByClient(clientId).stream()
                .anyMatch(loan -> loan.loanCategory() == category
                        && loan.amount().compareTo(amount) == 0
                        && loan.termMonths() == termMonths);

        if (!exists) {
            loanService.grantLoan(new GrantLoanRequest(clientId, category, amount, termMonths));
        }
    }
}
