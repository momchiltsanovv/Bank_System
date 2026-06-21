package org.example.bank_system.service;

import lombok.RequiredArgsConstructor;
import org.example.bank_system.dto.request.CreateCorporateClientRequest;
import org.example.bank_system.dto.request.CreateIndividualClientRequest;
import org.example.bank_system.dto.response.CorporateClientResponse;
import org.example.bank_system.dto.response.IndividualClientResponse;
import org.example.bank_system.entity.AccountStatus;
import org.example.bank_system.entity.BankAccount;
import org.example.bank_system.entity.CorporateClient;
import org.example.bank_system.entity.IndividualClient;
import org.example.bank_system.exception.BusinessRuleException;
import org.example.bank_system.exception.ResourceNotFoundException;
import org.example.bank_system.repository.BankAccountRepository;
import org.example.bank_system.repository.ClientRepository;
import org.example.bank_system.repository.CorporateClientRepository;
import org.example.bank_system.repository.IndividualClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final IndividualClientRepository individualRepo;
    private final CorporateClientRepository corporateRepo;
    private final ClientRepository clientRepo;
    private final BankAccountRepository accountRepo;

    @Transactional
    public IndividualClientResponse createIndividual(CreateIndividualClientRequest req) {
        if (individualRepo.existsByEgn(req.egn())) {
            throw new BusinessRuleException("Individual client with EGN " + req.egn() + " already exists");
        }
        IndividualClient client = new IndividualClient();
        client.setFirstName(req.firstName());
        client.setLastName(req.lastName());
        client.setEgn(req.egn());
        client = individualRepo.save(client);
        createDefaultAccount(client);
        return toResponse(client);
    }

    @Transactional
    public CorporateClientResponse createCorporate(CreateCorporateClientRequest req) {
        if (corporateRepo.existsByEik(req.eik())) {
            throw new BusinessRuleException("Corporate client with EIK " + req.eik() + " already exists");
        }
        CorporateClient client = new CorporateClient();
        client.setCompanyName(req.companyName());
        client.setEik(req.eik());
        client.setRepresentativeFirstName(req.representativeFirstName());
        client.setRepresentativeLastName(req.representativeLastName());
        client = corporateRepo.save(client);
        createDefaultAccount(client);
        return toResponse(client);
    }

    @Transactional(readOnly = true)
    public List<IndividualClientResponse> getAllIndividuals() {
        return individualRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CorporateClientResponse> getAllCorporate() {
        return corporateRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public IndividualClientResponse getIndividualById(Long id) {
        return toResponse(individualRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Individual client not found: " + id)));
    }

    @Transactional(readOnly = true)
    public CorporateClientResponse getCorporateById(Long id) {
        return toResponse(corporateRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found: " + id)));
    }

    @Transactional(readOnly = true)
    public IndividualClientResponse getIndividualByEgn(String egn) {
        return toResponse(individualRepo.findByEgn(egn)
                .orElseThrow(() -> new ResourceNotFoundException("Individual client not found with EGN: " + egn)));
    }

    @Transactional(readOnly = true)
    public CorporateClientResponse getCorporateByEik(String eik) {
        return toResponse(corporateRepo.findByEik(eik)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found with EIK: " + eik)));
    }

    private void createDefaultAccount(org.example.bank_system.entity.Client client) {
        BankAccount account = new BankAccount();
        account.setIban(generateIban());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setOwner(client);
        accountRepo.save(account);
    }

    private String generateIban() {
        String bban = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        return "BG00" + bban;
    }

    // package-private helper used by other services
    org.example.bank_system.entity.Client getClientEntityById(Long id) {
        return clientRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
    }

    private IndividualClientResponse toResponse(IndividualClient c) {
        return new IndividualClientResponse(c.getId(), c.getFirstName(), c.getLastName(), c.getEgn());
    }

    private CorporateClientResponse toResponse(CorporateClient c) {
        return new CorporateClientResponse(c.getId(), c.getCompanyName(), c.getEik(),
                c.getRepresentativeFirstName(), c.getRepresentativeLastName());
    }
}
