package com.bradox.erp;

import com.bradox.erp.platform.bootstrap.PlatformRbacSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1 of the sales correction use cases (shared doc "Sales Corrections: Use Cases"): guards that
 * keep orders, deliveries and invoices consistent. Test names carry the doc's case numbers, and every
 * test ends by asserting the consistency checker finds nothing wrong with its order.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SalesCorrectionsPhase1Test extends SalesScenarioSupport {

    // ---------------------------------------------------------------- B. confirmed, nothing done

    @Test
    void case07_increaseQuantity_updatesTheSingleOpenDelivery() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = confirm(createOrder(line(null, product, 10, 100)));

        so = amend(so, line(lineId(so, 0), product, 15, 100)).andExpect(status().isOk()).json();

        assertThat(openDeliveries(so)).hasSize(1);
        assertThat(openDemand(so, lineId(so, 0))).isEqualByComparingTo("15");
        assertConsistent(so);
    }

    @Test
    void case08_decreaseQuantity_reducesTheOpenDelivery() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = confirm(createOrder(line(null, product, 10, 100)));

        so = amend(so, line(lineId(so, 0), product, 4, 100)).andExpect(status().isOk()).json();

        assertThat(openDeliveries(so)).hasSize(1);
        assertThat(openDemand(so, lineId(so, 0))).isEqualByComparingTo("4");
        assertConsistent(so);
    }

    @Test
    void case09_removeLine_dropsItFromTheOpenDelivery() throws Exception {
        UUID a = stockedProduct(50);
        UUID b = stockedProduct(50);
        JsonNode so = confirm(createOrder(line(null, a, 5, 100), line(null, b, 6, 100)));
        String keep = lineId(so, 0);
        String removed = lineId(so, 1);

        so = amend(so, line(keep, a, 5, 100)).andExpect(status().isOk()).json();

        assertThat(openDeliveries(so)).hasSize(1);
        assertThat(openDemand(so, keep)).isEqualByComparingTo("5");
        assertThat(openDemand(so, removed)).isEqualByComparingTo("0");
        assertConsistent(so);
    }

    @Test
    void case14_cancelConfirmedOrder_cancelsOpenDeliveries() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = confirm(createOrder(line(null, product, 3, 100)));

        so = call(post(orderUrl(so) + "/cancel")).andExpect(status().isOk()).json();

        assertThat(so.get("state").asText()).isEqualTo("CANCELLED");
        assertThat(openDeliveries(so)).isEmpty();
        assertConsistent(so);
    }

    // ---------------------------------------------------------------- C. delivered, not invoiced

    @Test
    void case13_changeCustomerAfterDelivery_isRefused() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = deliverAll(confirm(createOrder(line(null, product, 2, 100))));
        UUID other = createCustomer();

        call(put(orderUrl(so)).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(other, so.get("rowVersion").asLong(), line(lineId(so, 0), product, 2, 100))))
                .refusedWith("Cannot change customer");
        assertConsistent(so);
    }

    @Test
    void case17_increaseAfterPartialDelivery_updatesTheBackorder() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = confirm(createOrder(line(null, product, 10, 100)));
        UUID delivery = openDeliveries(so).get(0);
        validate(delivery, firstMoveId(delivery), "4", true);
        so = reload(so);

        so = amend(so, line(lineId(so, 0), product, 12, 100)).andExpect(status().isOk()).json();

        assertThat(so.get("lines").get(0).get("qtyDelivered").decimalValue()).isEqualByComparingTo("4");
        assertThat(openDeliveries(so)).hasSize(1);
        assertThat(openDemand(so, lineId(so, 0))).isEqualByComparingTo("8");
        assertConsistent(so);
    }

    @Test
    void case20_decreaseBelowDelivered_isRefusedAndChangesNothing() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = deliverAll(confirm(createOrder(line(null, product, 6, 100))));
        long version = so.get("rowVersion").asLong();

        amend(so, line(lineId(so, 0), product, 2, 100)).refusedWith("below the delivered quantity");

        JsonNode after = reload(so);
        assertThat(after.get("rowVersion").asLong()).isEqualTo(version);
        assertThat(after.get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("6");
        assertConsistent(after);
    }

    @Test
    void case23_deliverMoreThanOrdered_isRefused() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = confirm(createOrder(line(null, product, 3, 100)));
        UUID delivery = openDeliveries(so).get(0);

        mockMvc.perform(post("/api/v1/inventory/pickings/" + delivery + "/validate")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"picks\":[{\"moveId\":\"" + firstMoveId(delivery) + "\",\"pickedQuantity\":5}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("more than ordered")));

        assertThat(reload(so).get("lines").get(0).get("qtyDelivered").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(so);
    }

    /**
     * Production case SO/2026/00080: goods came back to be replaced, then the return was itself
     * "returned" twice (double click). The second one must be refused.
     */
    @Test
    void case23_returningTheSamePickingTwice_isRefused() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = deliverAll(confirm(createOrder(line(null, product, 1, 100))));
        UUID delivery = UUID.fromString(so.get("deliveryPickingIds").get(0).asText());

        UUID ret = returnPicking(delivery, false).andExpect(status().isOk()).id();
        validate(ret, null, null, false);
        UUID reDelivery = returnPicking(ret).andExpect(status().isOk()).id();

        returnPicking(ret).refusedWith("already been returned");
        validate(reDelivery, null, null, false);

        assertThat(reload(so).get("lines").get(0).get("qtyDelivered").decimalValue()).isEqualByComparingTo("1");
        assertConsistent(so);
    }

    @Test
    void case25_closeRemaining_setsOrderedToDeliveredAndCancelsTheBackorder() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = confirm(createOrder(line(null, product, 10, 100)));
        UUID delivery = openDeliveries(so).get(0);
        validate(delivery, firstMoveId(delivery), "6", true);

        so = call(post(orderUrl(so) + "/close-remaining").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"customer refused the rest\"}")).andExpect(status().isOk()).json();

        assertThat(so.get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("6");
        assertThat(openDeliveries(so)).isEmpty();
        assertConsistent(so);
    }

    @Test
    void case26_cancelWithDeliveredGoods_isRefusedUntilReturned() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = deliverAll(confirm(createOrder(line(null, product, 2, 100))));

        call(post(orderUrl(so) + "/cancel")).refusedWith("goods have been delivered");

        UUID delivery = UUID.fromString(so.get("deliveryPickingIds").get(0).asText());
        validate(returnPicking(delivery).andExpect(status().isOk()).id(), null, null, false);
        so = call(post(orderUrl(so) + "/cancel")).andExpect(status().isOk()).json();
        assertThat(so.get("state").asText()).isEqualTo("CANCELLED");
        assertConsistent(so);
    }

    // ---------------------------------------------------------------- D. draft invoice

    @Test
    void case27_case29_draftInvoiceBlocksLineEditsUntilDeleted() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = deliverAll(confirm(createOrder(line(null, product, 4, 100))));
        UUID draft = createInvoiceFromOrder(so);
        so = reload(so);

        amend(so, line(lineId(so, 0), product, 9, 100)).refusedWith("draft invoice or credit note exists");

        call(delete("/api/v1/accounting/customer-invoices/" + draft)).andExpect(status().isNoContent());
        so = amend(reload(so), line(lineId(so, 0), product, 9, 100)).andExpect(status().isOk()).json();
        assertThat(so.get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("9");
        assertConsistent(so);
    }

    @Test
    void case28_postingAnInvoiceBeyondTheOrderedQuantity_isRefused() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = deliverAll(confirm(createOrder(line(null, product, 2, 100))));
        String body = "{\"customerPartnerId\":\"" + customer + "\",\"invoiceDate\":\"2026-05-04\","
                + "\"dueDate\":\"2026-06-04\",\"currencyCode\":\"USD\",\"salesOrderId\":\"" + so.get("id").asText()
                + "\",\"lines\":[{\"name\":\"Too many\",\"qty\":5,\"unitPrice\":100,\"salesOrderLineId\":\""
                + lineId(so, 0) + "\"}]}";
        UUID invoice = call(post("/api/v1/accounting/customer-invoices").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isOk()).id();

        call(post("/api/v1/accounting/customer-invoices/" + invoice + "/post"))
                .refusedWith("Cannot invoice more than ordered");
        call(delete("/api/v1/accounting/customer-invoices/" + invoice)).andExpect(status().isNoContent());
        assertConsistent(so);
    }

    // ---------------------------------------------------------------- E. posted invoice (R1, R2)

    @Test
    void ruleR2_invoicedLine_priceAndDiscountAreLocked_quantityCannotDropBelowInvoiced() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));

        amend(so, line(lineId(so, 0), product, 5, 90)).refusedWith("locked once a line is invoiced");
        amend(so, lineWithDiscount(lineId(so, 0), product, 5, 100, 10)).refusedWith("locked once a line is invoiced");
        amend(so, line(lineId(so, 0), product, 3, 100)).refusedWith("below the delivered quantity");

        // Increasing quantity stays allowed: the extra is delivered and invoiced later (case 38).
        so = amend(so, line(lineId(so, 0), product, 7, 100)).andExpect(status().isOk()).json();
        assertThat(openDemand(so, lineId(so, 0))).isEqualByComparingTo("2");
        assertConsistent(so);
    }

    @Test
    void ruleR1_serviceLineInvoicedWithoutDelivery_quantityCannotDropBelowInvoiced() throws Exception {
        UUID service = serviceProduct();
        JsonNode so = invoiceAll(confirm(createOrder(line(null, service, 4, 100))));
        assertThat(so.get("lines").get(0).get("qtyInvoiced").decimalValue()).isEqualByComparingTo("4");

        amend(so, line(lineId(so, 0), service, 2, 100)).refusedWith("below the invoiced quantity");
        assertConsistent(so);
    }

    // ---------------------------------------------------------------- G. concurrency

    @Test
    void case59_editWithAStaleVersion_isRejected() throws Exception {
        UUID product = stockedProduct(50);
        JsonNode so = confirm(createOrder(line(null, product, 2, 100)));
        long seen = so.get("rowVersion").asLong();

        call(put(orderUrl(so)).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(customer, seen, line(lineId(so, 0), product, 3, 100))))
                .andExpect(status().isOk());
        call(put(orderUrl(so)).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(customer, seen, line(lineId(so, 0), product, 4, 100))))
                .andExpect(status().isConflict());

        assertThat(reload(so).get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("3");
        assertConsistent(so);
    }

}
