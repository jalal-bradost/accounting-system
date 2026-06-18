package com.jalaldeveloper.accountingsystem;

import com.jalaldeveloper.accountingsystem.platform.bootstrap.PlatformRbacSeeder;
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
import java.math.RoundingMode;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MultiCurrencyPartnerStatementIntegrationTest {

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void iqd_invoice_shows_iqd_on_partner_statement_and_posts_company_ar() throws Exception {
        UUID arAccountId = accountIdByCode("430003");
        UUID customerId = createCustomer(arAccountId, "IQD");

        String invBody = "{\"customerPartnerId\":\"" + customerId + "\",\"invoiceDate\":\"2026-05-04\","
                + "\"currencyCode\":\"IQD\",\"reference\":\"INV-IQD\","
                + "\"lines\":[{\"name\":\"Goods\",\"qty\":1,\"unitPrice\":3000}]}";
        MvcResult createRes = mockMvc.perform(post("/api/v1/accounting/customer-invoices")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invBody))
                .andExpect(status().isOk())
                .andReturn();
        UUID invoiceId = UUID.fromString(json.readTree(createRes.getResponse().getContentAsString()).get("id").asText());

        MvcResult postRes = mockMvc.perform(post("/api/v1/accounting/customer-invoices/" + invoiceId + "/post")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode posted = json.readTree(postRes.getResponse().getContentAsString());
        BigDecimal invoiceRate = posted.get("exchangeRateToCompany").decimalValue();
        assertThat(invoiceRate).isNotEqualByComparingTo(BigDecimal.ONE);

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
        BigDecimal expectedCompany = new BigDecimal("3000").multiply(invoiceRate).setScale(4, RoundingMode.HALF_UP);
        assertThat(arDebit).isEqualByComparingTo(expectedCompany);

        MvcResult stmtRes = mockMvc.perform(get("/api/v1/accounting/partner-statement")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .param("partnerId", customerId.toString())
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode stmt = json.readTree(stmtRes.getResponse().getContentAsString());
        JsonNode sections = stmt.get("receivableSections");
        assertThat(sections.isArray()).isTrue();
        assertThat(sections.size()).isEqualTo(1);
        JsonNode iqdSection = sections.get(0);
        assertThat(iqdSection.get("currencyCode").asText()).isEqualTo("IQD");
        assertThat(iqdSection.get("closingBalance").decimalValue()).isEqualByComparingTo(new BigDecimal("3000.0000"));
        JsonNode line = iqdSection.get("lines").get(0);
        assertThat(line.get("currencyCode").asText()).isEqualTo("IQD");
        assertThat(line.get("debit").decimalValue()).isEqualByComparingTo(new BigDecimal("3000.0000"));
    }

  @Test
    void payment_at_different_rate_posts_exchange_difference() throws Exception {
        UUID arAccountId = accountIdByCode("430003");
        UUID gainAccountId = accountIdByCode("430014");
        UUID lossAccountId = accountIdByCode("430015");
        UUID customerId = createCustomer(arAccountId, "IQD");
        UUID cashJournalId = journalIdByType("CASH");

        String invBody = "{\"customerPartnerId\":\"" + customerId + "\",\"invoiceDate\":\"2026-05-04\","
                + "\"currencyCode\":\"IQD\",\"reference\":\"INV-FX\","
                + "\"lines\":[{\"name\":\"Goods\",\"qty\":1,\"unitPrice\":1310}]}";
        MvcResult createRes = mockMvc.perform(post("/api/v1/accounting/customer-invoices")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invBody))
                .andExpect(status().isOk())
                .andReturn();
        UUID invoiceId = UUID.fromString(json.readTree(createRes.getResponse().getContentAsString()).get("id").asText());
        mockMvc.perform(post("/api/v1/accounting/customer-invoices/" + invoiceId + "/post")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        BigDecimal paymentRate = BigDecimal.ONE.divide(new BigDecimal("1320"), 12, RoundingMode.HALF_UP);
        String payBody = "{\"customerInvoiceId\":\"" + invoiceId + "\",\"paymentJournalId\":\"" + cashJournalId
                + "\",\"paymentDate\":\"2026-05-05\",\"amount\":1310,\"currencyCode\":\"IQD\","
                + "\"exchangeRateToCompany\":" + paymentRate.toPlainString() + ",\"reference\":\"PAY-FX\"}";
        MvcResult payRes = mockMvc.perform(post("/api/v1/accounting/customer-invoices/payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk())
                .andReturn();
        UUID payJeId = UUID.fromString(json.readTree(payRes.getResponse().getContentAsString())
                .get("journalEntryId").asText());

        MvcResult jeResult = mockMvc.perform(get("/api/v1/journal-entries/" + payJeId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode je = json.readTree(jeResult.getResponse().getContentAsString());
        BigDecimal gainCredit = sumCredits(je, gainAccountId);
        BigDecimal lossDebit = sumDebits(je, lossAccountId);
        assertThat(gainCredit.signum() > 0 || lossDebit.signum() > 0).isTrue();
    }

    private BigDecimal sumCredits(JsonNode je, UUID accountId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode it : je.get("items")) {
            if (it.get("accountId").asText().equals(accountId.toString())) {
                sum = sum.add(it.get("credit").decimalValue());
            }
        }
        return sum;
    }

    private BigDecimal sumDebits(JsonNode je, UUID accountId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode it : je.get("items")) {
            if (it.get("accountId").asText().equals(accountId.toString())) {
                sum = sum.add(it.get("debit").decimalValue());
            }
        }
        return sum;
    }

    private UUID createCustomer(UUID receivableAccountId, String currencyCode) throws Exception {
        String body = "{\"kind\":\"COMPANY\",\"displayName\":\"Customer " + UUID.randomUUID().toString().substring(0, 6)
                + "\",\"customer\":true,\"vendor\":false,\"receivableAccountId\":\"" + receivableAccountId
                + "\",\"currencyCode\":\"" + currencyCode + "\"}";
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
