package org.example.bank_system.integration;

import com.jayway.jsonpath.JsonPath;
import org.example.bank_system.entity.BankAccount;
import org.example.bank_system.repository.BankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BankAccountControllerIT extends BaseIT {

    @Autowired private BankAccountRepository accountRepo;

    private long clientId;
    private long autoAccountId;

    @BeforeEach
    void createClient() throws Exception {
        String resp = mvc.perform(post("/api/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Test","lastName":"User","egn":"1234567890"}
                                """))
                .andReturn().getResponse().getContentAsString();
        clientId = ((Number) JsonPath.read(resp, "$.id")).longValue();

        String accounts = mvc.perform(get("/api/accounts/client/" + clientId))
                .andReturn().getResponse().getContentAsString();
        autoAccountId = ((Number) JsonPath.read(accounts, "$[0].id")).longValue();
    }

    @Test
    void openAccount_returns201() throws Exception {
        mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"iban\":\"BG80BNBG96611020345678\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.iban").value("BG80BNBG96611020345678"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.clientId").value(clientId));
    }

    @Test
    void openAccount_duplicateIban_returns422() throws Exception {
        mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"iban\":\"BG80BNBG96611020345678\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"iban\":\"BG80BNBG96611020345678\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void openAccount_invalidIban_returns400() throws Exception {
        mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"iban\":\"not-valid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void closeAccount_zeroBalance_returns200() throws Exception {
        mvc.perform(patch("/api/accounts/" + autoAccountId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void closeAccount_nonZeroBalance_returns422() throws Exception {
        BankAccount account = accountRepo.findById(autoAccountId).orElseThrow();
        account.setBalance(BigDecimal.valueOf(500));
        accountRepo.save(account);

        mvc.perform(patch("/api/accounts/" + autoAccountId + "/close"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("non-zero balance")));
    }

    @Test
    void closeAccount_alreadyClosed_returns422() throws Exception {
        mvc.perform(patch("/api/accounts/" + autoAccountId + "/close"))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/accounts/" + autoAccountId + "/close"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("already closed")));
    }

    @Test
    void getAccountsByClient_includesAutoCreatedAccount() throws Exception {
        mvc.perform(get("/api/accounts/client/" + clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(autoAccountId));
    }

    @Test
    void getAccountsByClient_afterOpeningExtra_returnsBoth() throws Exception {
        mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"iban\":\"BG22ZZZZ00009999999999\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/accounts/client/" + clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getAccountById_returnsAccount() throws Exception {
        mvc.perform(get("/api/accounts/" + autoAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(autoAccountId))
                .andExpect(jsonPath("$.clientId").value(clientId));
    }

    @Test
    void getAccountById_notFound_returns404() throws Exception {
        mvc.perform(get("/api/accounts/99999"))
                .andExpect(status().isNotFound());
    }
}
