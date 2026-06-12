package org.example.bank_system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bank_system.dto.request.OpenBankAccountRequest;
import org.example.bank_system.dto.response.BankAccountResponse;
import org.example.bank_system.service.BankAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccountResponse openAccount(@Valid @RequestBody OpenBankAccountRequest req) {
        return accountService.openAccount(req);
    }

    @PatchMapping("/{id}/close")
    public BankAccountResponse closeAccount(@PathVariable("id") Long id) {
        return accountService.closeAccount(id);
    }

    @GetMapping("/{id}")
    public BankAccountResponse getById(@PathVariable("id") Long id) {
        return accountService.getById(id);
    }

    @GetMapping("/client/{clientId}")
    public List<BankAccountResponse> getByClient(@PathVariable("clientId") Long clientId) {
        return accountService.getAccountsByClient(clientId);
    }
}
