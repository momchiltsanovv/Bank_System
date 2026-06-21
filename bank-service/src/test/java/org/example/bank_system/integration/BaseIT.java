package org.example.bank_system.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
abstract class BaseIT {

    @Autowired private WebApplicationContext wac;
    @Autowired private JdbcTemplate jdbc;

    protected MockMvc mvc;

    @BeforeEach
    void baseSetup() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).build();
        jdbc.execute("DELETE FROM repayment_instalments");
        jdbc.execute("DELETE FROM loans");
        jdbc.execute("DELETE FROM bank_accounts");
        jdbc.execute("DELETE FROM individual_clients");
        jdbc.execute("DELETE FROM corporate_clients");
        jdbc.execute("DELETE FROM clients");
    }
}
