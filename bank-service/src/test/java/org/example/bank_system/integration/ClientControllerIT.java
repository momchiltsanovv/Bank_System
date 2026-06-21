package org.example.bank_system.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClientControllerIT extends BaseIT {

    @Test
    void createIndividual_returns201WithFields() throws Exception {
        mvc.perform(post("/api/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ivan","lastName":"Ivanov","egn":"1234567890"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Ivan"))
                .andExpect(jsonPath("$.lastName").value("Ivanov"))
                .andExpect(jsonPath("$.egn").value("1234567890"));
    }

    @Test
    void createIndividual_autoCreatesBankAccount() throws Exception {
        String resp = mvc.perform(post("/api/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ivan","lastName":"Ivanov","egn":"1234567890"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long clientId = ((Number) JsonPath.read(resp, "$.id")).longValue();

        mvc.perform(get("/api/accounts/client/" + clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].balance").value(0))
                .andExpect(jsonPath("$[0].clientId").value(clientId))
                .andExpect(jsonPath("$[0].iban").isString());
    }

    @Test
    void createIndividual_duplicateEgn_returns422() throws Exception {
        String body = """
                {"firstName":"Ivan","lastName":"Ivanov","egn":"1234567890"}
                """;
        mvc.perform(post("/api/clients/individual")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/clients/individual")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createIndividual_blankFirstName_returns400() throws Exception {
        mvc.perform(post("/api/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Ivanov","egn":"1234567890"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createIndividual_invalidEgn_returns400() throws Exception {
        mvc.perform(post("/api/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ivan","lastName":"Ivanov","egn":"123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getIndividualById_returnsClient() throws Exception {
        String resp = mvc.perform(post("/api/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Maria","lastName":"Petrova","egn":"9876543210"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(resp, "$.id")).longValue();

        mvc.perform(get("/api/clients/individual/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Maria"))
                .andExpect(jsonPath("$.egn").value("9876543210"));
    }

    @Test
    void getIndividualById_notFound_returns404() throws Exception {
        mvc.perform(get("/api/clients/individual/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getIndividualByEgn_returnsClient() throws Exception {
        mvc.perform(post("/api/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Georgi","lastName":"Georgiev","egn":"5555555555"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/clients/individual/by-egn?egn=5555555555"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.egn").value("5555555555"));
    }

    @Test
    void getAllIndividuals_returnsAllCreated() throws Exception {
        mvc.perform(post("/api/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"A","lastName":"B","egn":"1111111111"}
                                """))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"C","lastName":"D","egn":"2222222222"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/clients/individual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void createCorporate_returns201WithFields() throws Exception {
        mvc.perform(post("/api/clients/corporate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName":"Acme Ltd","eik":"123456789","representativeFirstName":"Georgi","representativeLastName":"Petrov"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Acme Ltd"))
                .andExpect(jsonPath("$.eik").value("123456789"))
                .andExpect(jsonPath("$.representativeFirstName").value("Georgi"));
    }

    @Test
    void createCorporate_autoCreatesBankAccount() throws Exception {
        String resp = mvc.perform(post("/api/clients/corporate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName":"Test Corp","eik":"987654321","representativeFirstName":"Ana","representativeLastName":"Georgieva"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long clientId = ((Number) JsonPath.read(resp, "$.id")).longValue();

        mvc.perform(get("/api/accounts/client/" + clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void createCorporate_duplicateEik_returns422() throws Exception {
        String body = """
                {"companyName":"Acme Ltd","eik":"123456789","representativeFirstName":"Georgi","representativeLastName":"Petrov"}
                """;
        mvc.perform(post("/api/clients/corporate")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/clients/corporate")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getCorporateById_returnsClient() throws Exception {
        String resp = mvc.perform(post("/api/clients/corporate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName":"Beta Ltd","eik":"111222333","representativeFirstName":"Petar","representativeLastName":"Petrov"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(resp, "$.id")).longValue();

        mvc.perform(get("/api/clients/corporate/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Beta Ltd"));
    }

    @Test
    void getCorporateById_notFound_returns404() throws Exception {
        mvc.perform(get("/api/clients/corporate/99999"))
                .andExpect(status().isNotFound());
    }
}
