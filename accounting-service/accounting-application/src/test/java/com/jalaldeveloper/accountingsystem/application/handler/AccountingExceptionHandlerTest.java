package com.jalaldeveloper.accountingsystem.application.handler;

import com.jalaldeveloper.accountingsystem.application.rest.AccountController;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.AccountApplicationService;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class)
@Import({ AccountController.class, AccountingExceptionHandler.class })
class AccountingExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountApplicationService accountApplicationService;

    @Test
    void handleAccountingDomainException_returns422AndErrorBody() throws Exception {
        when(accountApplicationService.getAccount(any()))
                .thenThrow(new AccountingDomainException("Entry is not balanced!"));

        mockMvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACCOUNTING_DOMAIN_ERROR"))
                .andExpect(jsonPath("$.message").value("Entry is not balanced!"));
    }

    @Test
    void handleIllegalArgumentException_returns400() throws Exception {
        when(accountApplicationService.getAccount(any()))
                .thenThrow(new IllegalArgumentException("Invalid id."));

        mockMvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid id."));
    }
}
