package org.example.bank_system.integration;

import com.jayway.jsonpath.JsonPath;
import org.example.bank_system.entity.LoanCategory;
import org.example.bank_system.repository.LoanTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoanControllerIT extends BaseIT {

    @Autowired private LoanTypeRepository loanTypeRepo;

    private long clientId;
    private long consumerTypeId;

    @BeforeEach
    void setup() throws Exception {
        String resp = mvc.perform(post("/api/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Test","lastName":"Client","egn":"1234567890"}
                                """))
                .andReturn().getResponse().getContentAsString();
        clientId = ((Number) JsonPath.read(resp, "$.id")).longValue();

        consumerTypeId = loanTypeRepo.findByCategory(LoanCategory.CONSUMER).orElseThrow().getId();
    }

    @Test
    void grantLoan_returns201WithRepaymentPlan() throws Exception {
        mvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":%d,"loanCategory":"CONSUMER","amount":10000,"termMonths":12}
                                """.formatted(clientId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.loanCategory").value("CONSUMER"))
                .andExpect(jsonPath("$.amount").value(10000))
                .andExpect(jsonPath("$.termMonths").value(12))
                .andExpect(jsonPath("$.paidInstalments").value(0))
                .andExpect(jsonPath("$.totalInstalments").value(12))
                .andExpect(jsonPath("$.repaymentPlan", hasSize(12)));
    }

    @Test
    void grantLoan_lastInstalmentClearsRemainingBalance() throws Exception {
        String resp = mvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":%d,"loanCategory":"CONSUMER","amount":10000,"termMonths":12}
                                """.formatted(clientId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        double lastRemaining = ((Number) JsonPath.read(resp, "$.repaymentPlan[11].remainingBalance")).doubleValue();
        assert lastRemaining == 0.0 : "Last instalment remaining balance must be 0.00, got " + lastRemaining;
    }

    @Test
    void grantLoan_amountExceedsMax_returns422() throws Exception {
        mvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":%d,"loanCategory":"CONSUMER","amount":999999,"termMonths":12}
                                """.formatted(clientId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("exceeds maximum")));
    }

    @Test
    void grantLoan_termExceedsMax_returns422() throws Exception {
        mvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":%d,"loanCategory":"CONSUMER","amount":5000,"termMonths":999}
                                """.formatted(clientId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("exceeds maximum")));
    }

    @Test
    void grantLoan_clientNotFound_returns404() throws Exception {
        mvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":99999,"loanCategory":"CONSUMER","amount":5000,"termMonths":12}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLoan_returns200WithRepaymentPlan() throws Exception {
        long loanId = grantLoan(clientId, "CONSUMER", 10000, 12);

        mvc.perform(get("/api/loans/" + loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loanId))
                .andExpect(jsonPath("$.repaymentPlan", hasSize(12)));
    }

    @Test
    void getLoan_notFound_returns404() throws Exception {
        mvc.perform(get("/api/loans/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRepaymentPlan_returnsCorrectInstalments() throws Exception {
        long loanId = grantLoan(clientId, "CONSUMER", 10000, 6);

        mvc.perform(get("/api/loans/" + loanId + "/repayment-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].monthNumber").value(1))
                .andExpect(jsonPath("$[5].monthNumber").value(6))
                .andExpect(jsonPath("$[0].paid").value(false));
    }

    @Test
    void payInstalment_returns200AndMarkedPaid() throws Exception {
        long loanId = grantLoan(clientId, "CONSUMER", 10000, 12);

        mvc.perform(patch("/api/loans/" + loanId + "/instalments/1/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthNumber").value(1))
                .andExpect(jsonPath("$.paid").value(true));
    }

    @Test
    void payInstalment_alreadyPaid_returns422() throws Exception {
        long loanId = grantLoan(clientId, "CONSUMER", 10000, 12);

        mvc.perform(patch("/api/loans/" + loanId + "/instalments/1/pay"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/loans/" + loanId + "/instalments/1/pay"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("already paid")));
    }

    @Test
    void getLoanStatus_reflectsPaidInstalments() throws Exception {
        long loanId = grantLoan(clientId, "CONSUMER", 10000, 12);

        mvc.perform(get("/api/loans/" + loanId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidInstalments").value(0))
                .andExpect(jsonPath("$.totalInstalments").value(12))
                .andExpect(jsonPath("$.fullyPaid").value(false));

        mvc.perform(patch("/api/loans/" + loanId + "/instalments/1/pay"));
        mvc.perform(patch("/api/loans/" + loanId + "/instalments/2/pay"));

        mvc.perform(get("/api/loans/" + loanId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidInstalments").value(2))
                .andExpect(jsonPath("$.fullyPaid").value(false));
    }

    @Test
    void getLoanStatus_fullyPaidAfterAllInstalments() throws Exception {
        long loanId = grantLoan(clientId, "CONSUMER", 10000, 3);

        for (int month = 1; month <= 3; month++) {
            mvc.perform(patch("/api/loans/" + loanId + "/instalments/" + month + "/pay"))
                    .andExpect(status().isOk());
        }

        mvc.perform(get("/api/loans/" + loanId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidInstalments").value(3))
                .andExpect(jsonPath("$.fullyPaid").value(true));
    }

    @Test
    void getLoansByClient_returnsAllClientLoans() throws Exception {
        grantLoan(clientId, "CONSUMER", 5000, 6);
        grantLoan(clientId, "CONSUMER", 8000, 12);

        mvc.perform(get("/api/loans/client/" + clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getAllLoanTypes_returnsConsumerAndMortgage() throws Exception {
        mvc.perform(get("/api/loans/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].category", hasItems("CONSUMER", "MORTGAGE")));
    }

    @Test
    void updateLoanType_returns200WithNewValues() throws Exception {
        mvc.perform(patch("/api/loans/types/" + consumerTypeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"annualInterestRate":0.09,"maxAmount":60000,"maxTermMonths":96}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annualInterestRate").value(0.09))
                .andExpect(jsonPath("$.maxAmount").value(60000))
                .andExpect(jsonPath("$.maxTermMonths").value(96));
    }

    @Test
    void updateLoanType_reducingBelowExistingLoanAmount_returns422() throws Exception {
        grantLoan(clientId, "CONSUMER", 30000, 24);

        mvc.perform(patch("/api/loans/types/" + consumerTypeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"annualInterestRate":0.085,"maxAmount":10000,"maxTermMonths":84}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("maxAmount")));
    }

    @Test
    void updateLoanType_reducingBelowExistingLoanTerm_returns422() throws Exception {
        grantLoan(clientId, "CONSUMER", 5000, 60);

        mvc.perform(patch("/api/loans/types/" + consumerTypeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"annualInterestRate":0.085,"maxAmount":50000,"maxTermMonths":12}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("maxTermMonths")));
    }

    private long grantLoan(long clientId, String category, int amount, int termMonths) throws Exception {
        String resp = mvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":%d,"loanCategory":"%s","amount":%d,"termMonths":%d}
                                """.formatted(clientId, category, amount, termMonths)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resp, "$.id")).longValue();
    }
}
