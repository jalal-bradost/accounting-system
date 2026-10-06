package com.bradox.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** What a customer owes follows their invoices, credit notes and payments. */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerBalanceTest extends SalesScenarioSupport {

    private BigDecimal owed(UUID partner) throws Exception {
        JsonNode r = call(get("/api/v1/accounting/customer-balances").param("partnerId", partner.toString()))
                .andExpect(status().isOk()).json();
        for (JsonNode row : r.get("balances")) {
            if (partner.toString().equals(row.get("partnerId").asText())) return row.get("balance").decimalValue();
        }
        return BigDecimal.ZERO;
    }

    @Test
    void balanceFollowsInvoicesCreditNotesAndPayments() throws Exception {
        UUID partner = createCustomer();
        UUID product = stockedProduct(20);
        assertThat(owed(partner)).isEqualByComparingTo("0");

        String body = orderBody(partner, null, line(null, product, 5, 100));
        JsonNode so = call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/sales/orders")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).json();
        so = invoiceAll(deliverAll(confirm(so)));
        UUID invoice = postedInvoices(so).get(0);
        assertThat(owed(partner)).isEqualByComparingTo("500");

        pay(invoice, "200").andExpect(status().isOk());
        assertThat(owed(partner)).isEqualByComparingTo("300");

        returnGoods(so, true, false, lineId(so, 0), "1").andExpect(status().isOk());
        assertThat(owed(partner)).isEqualByComparingTo("200");
    }

    @Test
    void withoutAFilterEveryCustomerWithABalanceIsListed() throws Exception {
        UUID partner = createCustomer();
        UUID product = stockedProduct(20);
        String body = orderBody(partner, null, line(null, product, 1, 100));
        JsonNode so = call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/sales/orders")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).json();
        invoiceAll(deliverAll(confirm(so)));

        JsonNode all = call(get("/api/v1/accounting/customer-balances")).andExpect(status().isOk()).json();
        boolean found = false;
        for (JsonNode row : all.get("balances")) {
            if (partner.toString().equals(row.get("partnerId").asText())) found = true;
        }
        assertThat(found).isTrue();
        assertThat(all.get("currencyCode").asText()).isNotBlank();
    }
}
