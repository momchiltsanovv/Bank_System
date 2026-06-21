package org.example.bank_system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bank_system.dto.request.CreateCorporateClientRequest;
import org.example.bank_system.dto.request.CreateIndividualClientRequest;
import org.example.bank_system.dto.response.CorporateClientResponse;
import org.example.bank_system.dto.response.IndividualClientResponse;
import org.example.bank_system.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping("/individual")
    @ResponseStatus(HttpStatus.CREATED)
    public IndividualClientResponse createIndividual(@Valid @RequestBody CreateIndividualClientRequest req) {
        return clientService.createIndividual(req);
    }

    @PostMapping("/corporate")
    @ResponseStatus(HttpStatus.CREATED)
    public CorporateClientResponse createCorporate(@Valid @RequestBody CreateCorporateClientRequest req) {
        return clientService.createCorporate(req);
    }

    @GetMapping("/individual")
    public List<IndividualClientResponse> getAllIndividuals() {
        return clientService.getAllIndividuals();
    }

    @GetMapping("/corporate")
    public List<CorporateClientResponse> getAllCorporate() {
        return clientService.getAllCorporate();
    }

    @GetMapping("/individual/{id}")
    public IndividualClientResponse getIndividual(@PathVariable("id") Long id) {
        return clientService.getIndividualById(id);
    }

    @GetMapping("/individual/by-egn")
    public IndividualClientResponse getIndividualByEgn(@RequestParam("egn") String egn) {
        return clientService.getIndividualByEgn(egn);
    }

    @GetMapping("/corporate/{id}")
    public CorporateClientResponse getCorporate(@PathVariable("id") Long id) {
        return clientService.getCorporateById(id);
    }

    @GetMapping("/corporate/by-eik")
    public CorporateClientResponse getCorporateByEik(@RequestParam("eik") String eik) {
        return clientService.getCorporateByEik(eik);
    }
}
