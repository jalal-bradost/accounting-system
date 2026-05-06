package com.jalaldeveloper.accountingsystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JournalEntryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createAccount_andGetAccount_returnsSameData() throws Exception {
        UUID companyId = UUID.randomUUID();
        String createBody = "{\"companyId\":\"" + companyId + "\",\"code\":\"1000\",\"name\":\"Cash\",\"accountType\":\"BANK_AND_CASH\",\"active\":true}";

        String createResult = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").exists())
                .andReturn().getResponse().getContentAsString();
        String accountIdStr = createResult.replaceAll(".*\"accountId\":\"([^\"]+)\".*", "$1");
        UUID accountId = UUID.fromString(accountIdStr);

        mockMvc.perform(get("/api/v1/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.name").value("Cash"));
    }

    @Test
    void listAccounts_byCompanyId_returnsCreatedAccounts() throws Exception {
        UUID companyId = UUID.randomUUID();
        String body = "{\"companyId\":\"" + companyId + "\",\"code\":\"2000\",\"name\":\"Receivables\",\"accountType\":\"RECEIVABLE\",\"active\":true}";
        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/accounts").param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").value("2000"));
    }
}
