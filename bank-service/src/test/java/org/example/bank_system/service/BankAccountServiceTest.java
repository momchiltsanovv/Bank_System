package org.example.bank_system.service;

import org.example.bank_system.dto.request.OpenBankAccountRequest;
import org.example.bank_system.dto.response.BankAccountResponse;
import org.example.bank_system.entity.AccountStatus;
import org.example.bank_system.entity.BankAccount;
import org.example.bank_system.entity.IndividualClient;
import org.example.bank_system.exception.BusinessRuleException;
import org.example.bank_system.repository.BankAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock BankAccountRepository accountRepo;
    @Mock ClientService clientService;
    @InjectMocks BankAccountService bankAccountService;

    @Test
    void openAccount_success() {
        when(accountRepo.existsByIban("BG80BNBG96611020345678")).thenReturn(false);
        IndividualClient client = new IndividualClient();
        client.setFirstName("Maria");
        when(clientService.getClientEntityById(1L)).thenReturn(client);
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BankAccountResponse resp = bankAccountService.openAccount(
                new OpenBankAccountRequest(1L, "BG80BNBG96611020345678"));

        assertThat(resp.iban()).isEqualTo("BG80BNBG96611020345678");
        assertThat(resp.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(resp.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void openAccount_duplicateIban_throws() {
        when(accountRepo.existsByIban("BG80BNBG96611020345678")).thenReturn(true);

        assertThatThrownBy(() -> bankAccountService.openAccount(
                new OpenBankAccountRequest(1L, "BG80BNBG96611020345678")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void closeAccount_alreadyClosed_throws() {
        BankAccount account = new BankAccount();
        account.setStatus(AccountStatus.CLOSED);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> bankAccountService.closeAccount(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already closed");
    }
}
