package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.AccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateAccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.AccountApplicationService;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class)
@org.springframework.context.annotation.Import(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountApplicationService accountApplicationService;

    @Test
    void createAccount_returns200AndBody() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(accountApplicationService.createAccount(any()))
                .thenReturn(new CreateAccountResponse(accountId, "Account created."));
        String json = "{\"companyId\":\"" + companyId + "\",\"code\":\"1000\",\"name\":\"Cash\",\"accountType\":\"ASSET\",\"active\":true}";

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.message").value("Account created."));
    }

    @Test
    void getAccount_returns200AndBody() throws Exception {
        UUID id = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(accountApplicationService.getAccount(id))
                .thenReturn(new AccountResponse(id, companyId, "1000", "Cash", AccountType.ASSET, true));

        mockMvc.perform(get("/api/v1/accounts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.name").value("Cash"));
    }

    @Test
    void listAccounts_returns200AndArray() throws Exception {
        UUID companyId = UUID.randomUUID();
        when(accountApplicationService.listAccountsByCompany(companyId))
                .thenReturn(List.of(new AccountResponse(UUID.randomUUID(), companyId, "1000", "Cash", AccountType.ASSET, true)));

        mockMvc.perform(get("/api/v1/accounts").param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("1000"));
    }
}
