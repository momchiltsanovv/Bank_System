package org.example.bank_system.service;

import org.example.bank_system.dto.request.CreateCorporateClientRequest;
import org.example.bank_system.dto.request.CreateIndividualClientRequest;
import org.example.bank_system.dto.response.CorporateClientResponse;
import org.example.bank_system.dto.response.IndividualClientResponse;
import org.example.bank_system.entity.CorporateClient;
import org.example.bank_system.entity.IndividualClient;
import org.example.bank_system.exception.BusinessRuleException;
import org.example.bank_system.repository.ClientRepository;
import org.example.bank_system.repository.CorporateClientRepository;
import org.example.bank_system.repository.IndividualClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock IndividualClientRepository individualRepo;
    @Mock CorporateClientRepository corporateRepo;
    @Mock ClientRepository clientRepo;
    @InjectMocks ClientService clientService;

    @Test
    void createIndividual_success() {
        when(individualRepo.existsByEgn("1234567890")).thenReturn(false);
        IndividualClient saved = new IndividualClient();
        saved.setFirstName("Ivan");
        saved.setLastName("Ivanov");
        saved.setEgn("1234567890");
        // simulate id assignment
        var savedRef = new IndividualClient() {{ setFirstName("Ivan"); setLastName("Ivanov"); setEgn("1234567890"); }};
        when(individualRepo.save(any())).thenAnswer(inv -> {
            IndividualClient c = inv.getArgument(0);
            return c;
        });

        IndividualClientResponse resp = clientService.createIndividual(
                new CreateIndividualClientRequest("Ivan", "Ivanov", "1234567890"));

        assertThat(resp.firstName()).isEqualTo("Ivan");
        assertThat(resp.egn()).isEqualTo("1234567890");
    }

    @Test
    void createIndividual_duplicateEgn_throws() {
        when(individualRepo.existsByEgn("1234567890")).thenReturn(true);

        assertThatThrownBy(() -> clientService.createIndividual(
                new CreateIndividualClientRequest("Ivan", "Ivanov", "1234567890")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("1234567890");
    }

    @Test
    void createCorporate_success() {
        when(corporateRepo.existsByEik("123456789")).thenReturn(false);
        when(corporateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CorporateClientResponse resp = clientService.createCorporate(
                new CreateCorporateClientRequest("Acme Ltd", "123456789", "Georgi", "Petrov"));

        assertThat(resp.companyName()).isEqualTo("Acme Ltd");
        assertThat(resp.eik()).isEqualTo("123456789");
        assertThat(resp.representativeFirstName()).isEqualTo("Georgi");
    }

    @Test
    void createCorporate_duplicateEik_throws() {
        when(corporateRepo.existsByEik("123456789")).thenReturn(true);

        assertThatThrownBy(() -> clientService.createCorporate(
                new CreateCorporateClientRequest("Acme Ltd", "123456789", "Georgi", "Petrov")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("123456789");
    }
}
