package com.bradox.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Repair orders end to end: create, confirm, and the sale quotation that confirming creates unless under warranty. */
@SpringBootTest
@AutoConfigureMockMvc
class RepairApiIntegrationTest extends SalesScenarioSupport {

    private static final String BASE = "/api/v1/repair/orders";

    private String orderJson(UUID product, boolean warranty, UUID part) {
        return "{\"customerPartnerId\":\"" + customer + "\",\"productId\":\"" + product + "\",\"underWarranty\":" + warranty
                + ",\"scheduledDate\":\"2026-10-09T09:00:00Z\",\"parts\":["
                + (part == null ? "" : "{\"productId\":\"" + part + "\",\"qty\":2}") + "]}";
    }

    private JsonNode create(String body) throws Exception {
        return call(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).json();
    }

    @Test
    void confirmingCreatesSaleQuotationForTheParts() throws Exception {
        UUID product = serviceProduct();
        UUID part = serviceProduct();
        JsonNode order = create(orderJson(product, false, part));
        assertThat(order.get("status").asText()).isEqualTo("NEW");
        assertThat(order.get("reference").asText()).startsWith("RO/");
        assertThat(order.get("customerName").isNull()).isFalse();
        assertThat(order.get("parts").size()).isEqualTo(1);

        JsonNode confirmed = call(post(BASE + "/" + order.get("id").asText() + "/confirm")).andExpect(status().isOk()).json();
        assertThat(confirmed.get("status").asText()).isEqualTo("CONFIRMED");
        String saleId = confirmed.get("saleOrderId").asText();
        JsonNode sale = call(get("/api/v1/sales/orders/" + saleId)).andExpect(status().isOk()).json();
        assertThat(sale.get("state").asText()).isEqualTo("DRAFT");
        assertThat(sale.get("customerPartnerId").asText()).isEqualTo(customer.toString());
        assertThat(sale.get("lines").size()).isEqualTo(1);
        assertThat(sale.get("lines").get(0).get("productId").asText()).isEqualTo(part.toString());

        // The chatter of the repair order records the confirmation.
        JsonNode feed = call(get("/api/v1/activities").param("companyId", COMPANY_ID.toString())
                .param("model", "repair.order").param("recordId", order.get("id").asText())).andExpect(status().isOk()).json();
        boolean logged = false;
        for (JsonNode a : feed.get("content")) {
            logged |= a.path("body").asText().contains(sale.get("name").asText());
        }
        assertThat(logged).isTrue();

        // Confirmed orders are locked.
        call(put(BASE + "/" + order.get("id").asText()).contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(product, false, part))).andExpect(status().is4xxClientError());
        JsonNode done = call(post(BASE + "/" + order.get("id").asText() + "/done")).andExpect(status().isOk()).json();
        assertThat(done.get("status").asText()).isEqualTo("DONE");
    }

    @Test
    void underWarrantyConfirmsWithoutQuotation() throws Exception {
        JsonNode order = create(orderJson(serviceProduct(), true, serviceProduct()));
        JsonNode confirmed = call(post(BASE + "/" + order.get("id").asText() + "/confirm")).andExpect(status().isOk()).json();
        assertThat(confirmed.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(confirmed.get("saleOrderId").isNull()).isTrue();

        boolean listed = false;
        for (JsonNode o : call(get(BASE)).andExpect(status().isOk()).json()) {
            listed |= o.get("id").asText().equals(order.get("id").asText());
        }
        assertThat(listed).isTrue();
    }

    @Test
    void notUnderWarrantyWithoutPartsIsRefused() throws Exception {
        JsonNode order = create(orderJson(serviceProduct(), false, null));
        call(post(BASE + "/" + order.get("id").asText() + "/confirm")).andExpect(status().is4xxClientError());
    }
}
