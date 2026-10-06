package com.bradox.erp;

import com.bradox.erp.platform.bootstrap.PlatformRbacSeeder;
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

/**
 * Payment cutover: register without an invoice, allocate partially, then reverse the allocation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerPaymentAllocationApiIntegrationTest {

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void register_unallocated_partial_allocate_then_deallocate() throws Exception {
        UUID arAccountId = accountIdByCode("430003");
        UUID customerId = createCustomer(arAccountId);
        UUID cashJournalId = journalIdByType("CASH");

        String invBody = "{\"customerPartnerId\":\"" + customerId + "\",\"invoiceDate\":\"2026-05-04\","
                + "\"currencyCode\":\"USD\",\"reference\":\"INV-ALLOC\","
                + "\"lines\":[{\"name\":\"Service\",\"qty\":1,\"unitPrice\":100}]}";
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

        String payBody = "{\"customerPartnerId\":\"" + customerId + "\",\"paymentJournalId\":\"" + cashJournalId
                + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":100,\"currencyCode\":\"USD\","
                + "\"reference\":\"PAY-UNALLOC\"}";
        MvcResult payRes = mockMvc.perform(post("/api/v1/accounting/customer-payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pay = json.readTree(payRes.getResponse().getContentAsString());
        UUID paymentId = UUID.fromString(pay.get("id").asText());
        assertThat(pay.get("allocatedAmount").decimalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pay.get("unallocatedAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(pay.get("allocations")).isEmpty();

        String allocBody = "{\"allocations\":[{\"invoiceId\":\"" + invoiceId + "\",\"amount\":40}]}";
        MvcResult allocRes = mockMvc.perform(post("/api/v1/accounting/customer-payments/" + paymentId + "/allocations")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(allocBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode allocated = json.readTree(allocRes.getResponse().getContentAsString());
        assertThat(allocated.get("allocatedAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("40"));
        assertThat(allocated.get("unallocatedAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("60"));
        assertThat(allocated.get("allocations")).hasSize(1);
        JsonNode alloc = allocated.get("allocations").get(0);
        UUID allocationId = UUID.fromString(alloc.get("id").asText());
        assertThat(alloc.get("customerInvoiceId").asText()).isEqualTo(invoiceId.toString());
        assertThat(alloc.get("amount").decimalValue()).isEqualByComparingTo(new BigDecimal("40"));
        assertThat(alloc.get("state").asText()).isEqualTo("ACTIVE");

        MvcResult invRes = mockMvc.perform(get("/api/v1/accounting/customer-invoices/" + invoiceId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode invoice = json.readTree(invRes.getResponse().getContentAsString());
        assertThat(invoice.get("amountPaid").decimalValue()).isEqualByComparingTo(new BigDecimal("40"));
        assertThat(invoice.get("amountResidual").decimalValue()).isEqualByComparingTo(new BigDecimal("60"));

        MvcResult deallocRes = mockMvc.perform(
                        post("/api/v1/accounting/customer-payments/allocations/" + allocationId + "/reverse")
                                .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode afterDealloc = json.readTree(deallocRes.getResponse().getContentAsString());
        assertThat(afterDealloc.get("allocatedAmount").decimalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(afterDealloc.get("unallocatedAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(afterDealloc.get("allocations").get(0).get("state").asText()).isEqualTo("REVERSED");

        invRes = mockMvc.perform(get("/api/v1/accounting/customer-invoices/" + invoiceId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        invoice = json.readTree(invRes.getResponse().getContentAsString());
        assertThat(invoice.get("amountPaid").decimalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(invoice.get("amountResidual").decimalValue()).isEqualByComparingTo(new BigDecimal("100"));
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
