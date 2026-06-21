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
import java.util.List;
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

    @Test
    void getAccountsByClient_returnsMappedAccounts() {
        IndividualClient client = new IndividualClient();
        client.setFirstName("Ana");

        BankAccount a1 = new BankAccount();
        a1.setIban("BG11AAAA00001111111111");
        a1.setBalance(BigDecimal.valueOf(1000));
        a1.setStatus(AccountStatus.ACTIVE);
        a1.setOwner(client);

        BankAccount a2 = new BankAccount();
        a2.setIban("BG22BBBB00002222222222");
        a2.setBalance(BigDecimal.valueOf(500));
        a2.setStatus(AccountStatus.CLOSED);
        a2.setOwner(client);

        when(accountRepo.findByOwnerId(5L)).thenReturn(List.of(a1, a2));

        var result = bankAccountService.getAccountsByClient(5L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).iban()).isEqualTo("BG11AAAA00001111111111");
        assertThat(result.get(1).status()).isEqualTo(AccountStatus.CLOSED);
    }
}
