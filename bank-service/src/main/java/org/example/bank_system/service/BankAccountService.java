package org.example.bank_system.service;

import lombok.RequiredArgsConstructor;
import org.example.bank_system.dto.request.OpenBankAccountRequest;
import org.example.bank_system.dto.response.BankAccountResponse;
import org.example.bank_system.entity.AccountStatus;
import org.example.bank_system.entity.BankAccount;
import org.example.bank_system.entity.Client;
import org.example.bank_system.exception.BusinessRuleException;
import org.example.bank_system.exception.ResourceNotFoundException;
import org.example.bank_system.repository.BankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository accountRepo;
    private final ClientService clientService;

    @Transactional
    public BankAccountResponse openAccount(OpenBankAccountRequest req) {
        if (accountRepo.existsByIban(req.iban())) {
            throw new BusinessRuleException("Account with IBAN " + req.iban() + " already exists");
        }
        Client owner = clientService.getClientEntityById(req.clientId());
        BankAccount account = new BankAccount();
        account.setIban(req.iban());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setOwner(owner);
        account = accountRepo.save(account);
        return toResponse(account);
    }

    @Transactional
    public BankAccountResponse closeAccount(Long accountId) {
        BankAccount account = findAccount(accountId);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessRuleException("Account is already closed");
        }
        if (account.getBalance().signum() > 0) {
            throw new BusinessRuleException("Cannot close account with non-zero balance: " + account.getBalance());
        }
        account.setStatus(AccountStatus.CLOSED);
        return toResponse(accountRepo.save(account));
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> getAccountsByClient(Long clientId) {
        return accountRepo.findByOwnerId(clientId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BankAccountResponse getById(Long id) {
        return toResponse(findAccount(id));
    }

    private BankAccount findAccount(Long id) {
        return accountRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found: " + id));
    }

    private BankAccountResponse toResponse(BankAccount a) {
        return new BankAccountResponse(a.getId(), a.getIban(), a.getBalance(),
                a.getStatus(), a.getOwner().getId());
    }
}
