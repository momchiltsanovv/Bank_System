package org.example.bank_system.service;

import lombok.RequiredArgsConstructor;
import org.example.bank_system.dto.request.CreateCorporateClientRequest;
import org.example.bank_system.dto.request.CreateIndividualClientRequest;
import org.example.bank_system.dto.response.CorporateClientResponse;
import org.example.bank_system.dto.response.IndividualClientResponse;
import org.example.bank_system.entity.CorporateClient;
import org.example.bank_system.entity.IndividualClient;
import org.example.bank_system.exception.BusinessRuleException;
import org.example.bank_system.exception.ResourceNotFoundException;
import org.example.bank_system.repository.ClientRepository;
import org.example.bank_system.repository.CorporateClientRepository;
import org.example.bank_system.repository.IndividualClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final IndividualClientRepository individualRepo;
    private final CorporateClientRepository corporateRepo;
    private final ClientRepository clientRepo;

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
