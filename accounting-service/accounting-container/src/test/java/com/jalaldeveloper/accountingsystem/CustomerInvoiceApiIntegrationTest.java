package com.jalaldeveloper.accountingsystem;

import com.jalaldeveloper.accountingsystem.web.DashboardController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerInvoiceApiIntegrationTest {

    private static final UUID COMPANY_ID = DashboardController.DEFAULT_COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void create_post_invoice_register_cash_payment_reconciles_ar() throws Exception {
        UUID arAccountId = accountIdByCode("430003");
        UUID customerId = createCustomer(arAccountId);
        UUID cashJournalId = journalIdByType("CASH");

        String invBody = "{\"customerPartnerId\":\"" + customerId + "\",\"invoiceDate\":\"2026-05-04\","
                + "\"currencyCode\":\"USD\",\"reference\":\"INV-TEST\","
                + "\"lines\":[{\"name\":\"Service\",\"qty\":2,\"unitPrice\":50}]}";
        MvcResult createRes = mockMvc.perform(post("/api/v1/accounting/customer-invoices")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode created = json.readTree(createRes.getResponse().getContentAsString());
        UUID invoiceId = UUID.fromString(created.get("id").asText());
        assertThat(created.get("state").asText()).isEqualTo("DRAFT");

        MvcResult postRes = mockMvc.perform(post("/api/v1/accounting/customer-invoices/" + invoiceId + "/post")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode posted = json.readTree(postRes.getResponse().getContentAsString());
        assertThat(posted.get("state").asText()).isEqualTo("POSTED");
        UUID journalEntryId = UUID.fromString(posted.get("journalEntryId").asText());

        MvcResult jeResult = mockMvc.perform(get("/api/v1/journal-entries/" + journalEntryId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode je = json.readTree(jeResult.getResponse().getContentAsString());
        BigDecimal arDebit = BigDecimal.ZERO;
        for (JsonNode it : je.get("items")) {
            if (it.get("accountId").asText().equals(arAccountId.toString())) {
                arDebit = arDebit.add(it.get("debit").decimalValue());
            }
        }
        assertThat(arDebit).isEqualByComparingTo(new BigDecimal("100.0000"));

        String payBody = "{\"customerInvoiceId\":\"" + invoiceId + "\",\"paymentJournalId\":\"" + cashJournalId
                + "\",\"paymentDate\":\"2026-05-04\",\"amount\":" + arDebit.toPlainString()
                + ",\"currencyCode\":\"USD\",\"reference\":\"PAY-TEST\"}";
        MvcResult payRes = mockMvc.perform(post("/api/v1/accounting/customer-invoices/payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pay = json.readTree(payRes.getResponse().getContentAsString());
        assertThat(pay.get("reconciliationId").asText()).isNotBlank();

        MvcResult listPayRes = mockMvc.perform(get("/api/v1/accounting/customer-invoices/payments")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pays = json.readTree(listPayRes.getResponse().getContentAsString());
        assertThat(pays.isArray()).isTrue();
        assertThat(pays.size()).isPositive();
        boolean found = false;
        for (JsonNode payRow : pays) {
            if (invoiceId.toString().equals(payRow.get("customerInvoiceId").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    private UUID createCustomer(UUID receivableAccountId) throws Exception {
        String body = "{\"kind\":\"COMPANY\",\"displayName\":\"Customer " + UUID.randomUUID().toString().substring(0, 6)
                + "\",\"customer\":true,\"vendor\":false,\"receivableAccountId\":\"" + receivableAccountId
                + "\",\"currencyCode\":\"USD\"}";
        MvcResult r = mockMvc.perform(post("/api/v1/contacts/partners")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(json.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID accountIdByCode(String code) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/accounts").param("companyId", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode n : json.readTree(r.getResponse().getContentAsString())) {
            if (code.equals(n.get("code").asText())) {
                return UUID.fromString(n.get("id").asText());
            }
        }
        throw new AssertionError("account not found: " + code);
    }

    private UUID journalIdByType(String type) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/journals").param("companyId", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode n : json.readTree(r.getResponse().getContentAsString())) {
            if (type.equalsIgnoreCase(n.get("journalType").asText())) {
                return UUID.fromString(n.get("id").asText());
            }
        }
        throw new AssertionError("journal type not found: " + type);
    }
}
