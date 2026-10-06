package com.bradox.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phases 3 and 4 of the sales correction use cases. Changing terms after invoicing credits the
 * invoiced quantity at the old terms and re-invoices it at the new ones; the guided flows reduce,
 * cancel or move an invoiced order with all their documents in one step. Every test checks that
 * posted invoices minus posted credit notes equal what the order now says is owed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SalesCorrectionsPhase3And4Test extends SalesScenarioSupport {

    // ---------------------------------------------------------------- phase 3: terms after invoicing

    @Test
    void case33_lowerPriceAfterInvoicing_creditsOldAndReinvoicesNew() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));

        JsonNode result = correction(so, "change-terms", false,
                "{\"lines\":[{\"salesOrderLineId\":\"" + lineId(so, 0) + "\",\"unitPrice\":80}],\"reason\":\"agreed\"}")
                .andExpect(status().isOk()).json();

        assertThat(result.get("documents")).hasSize(2);
        JsonNode l = result.get("order").get("lines").get(0);
        assertThat(l.get("unitPrice").decimalValue()).isEqualByComparingTo("80");
        assertThat(l.get("qtyInvoiced").decimalValue()).isEqualByComparingTo("5");
        assertThat(netInvoiced(so)).isEqualByComparingTo("400");
        assertConsistent(so);
    }

    @Test
    void case34_raisePriceAfterInvoicing() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 4, 100)))));

        correction(so, "change-terms", false,
                "{\"lines\":[{\"salesOrderLineId\":\"" + lineId(so, 0) + "\",\"unitPrice\":125}]}")
                .andExpect(status().isOk());

        assertThat(netInvoiced(so)).isEqualByComparingTo("500");
        assertConsistent(so);
    }

    @Test
    void case35_changeLineDiscountAfterInvoicing() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));

        correction(so, "change-terms", false,
                "{\"lines\":[{\"salesOrderLineId\":\"" + lineId(so, 0)
                        + "\",\"discountType\":\"PERCENT\",\"discountValue\":10}]}")
                .andExpect(status().isOk());

        assertThat(netInvoiced(so)).isEqualByComparingTo("450");
        assertConsistent(so);
    }

    @Test
    void case36_changeOrderDiscountAfterInvoicing_reinvoicesEveryInvoicedLine() throws Exception {
        UUID a = stockedProduct(20);
        UUID b = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, a, 2, 100), line(null, b, 3, 100)))));

        correction(so, "change-terms", false, "{\"orderDiscountType\":\"FIXED\",\"orderDiscountValue\":50}")
                .andExpect(status().isOk());

        assertThat(netInvoiced(so)).isEqualByComparingTo("450");
        assertConsistent(so);
    }

    @Test
    void case37_addTaxAfterInvoicing() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID tax = saleTax();

        correction(so, "change-terms", false,
                "{\"lines\":[{\"salesOrderLineId\":\"" + lineId(so, 0) + "\",\"taxIds\":[\"" + tax + "\"]}]}")
                .andExpect(status().isOk());

        assertThat(netInvoiced(so)).isEqualByComparingTo("550");
        assertConsistent(so);
    }

    @Test
    void preview_showsTheDocumentsButSavesNothing() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        int documentsBefore = documents(so);
        long version = reload(so).get("rowVersion").asLong();

        JsonNode preview = correction(so, "change-terms", true,
                "{\"lines\":[{\"salesOrderLineId\":\"" + lineId(so, 0) + "\",\"unitPrice\":80}]}")
                .andExpect(status().isOk()).json();

        assertThat(preview.get("preview").asBoolean()).isTrue();
        assertThat(preview.get("documents")).hasSize(2);
        assertThat(preview.get("order").get("lines").get(0).get("unitPrice").decimalValue()).isEqualByComparingTo("80");
        JsonNode after = reload(so);
        assertThat(after.get("lines").get(0).get("unitPrice").decimalValue()).isEqualByComparingTo("100");
        assertThat(after.get("rowVersion").asLong()).isEqualTo(version);
        assertThat(documents(so)).isEqualTo(documentsBefore);
        assertThat(netInvoiced(so)).isEqualByComparingTo("500");
        assertConsistent(so);
    }

    // ---------------------------------------------------------------- phase 4: guided flows

    /** Case 39 with a service: invoiced on the ordered quantity, nothing to deliver. */
    @Test
    void case39_reduceInvoicedQuantityBeforeDelivery_creditsTheDifference() throws Exception {
        UUID service = serviceProduct();
        JsonNode so = invoiceAll(confirm(createOrder(line(null, service, 4, 100))));

        JsonNode result = correction(so, "reduce-quantities", false,
                "{\"lines\":[{\"salesOrderLineId\":\"" + lineId(so, 0) + "\",\"qty\":1}]}")
                .andExpect(status().isOk()).json();

        JsonNode l = result.get("order").get("lines").get(0);
        assertThat(l.get("qtyOrdered").decimalValue()).isEqualByComparingTo("1");
        assertThat(l.get("qtyInvoiced").decimalValue()).isEqualByComparingTo("1");
        assertThat(netInvoiced(so)).isEqualByComparingTo("100");
        assertConsistent(so);
    }

    @Test
    void case39_reduceBelowDelivered_isRefused() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 4, 100)))));

        correction(so, "reduce-quantities", false,
                "{\"lines\":[{\"salesOrderLineId\":\"" + lineId(so, 0) + "\",\"qty\":2}]}")
                .refusedWith("below the delivered quantity");
        assertConsistent(so);
    }

    /** The INV/2026/00048 case: invoiced, not delivered, then cancelled properly. */
    @Test
    void case41_cancelAnInvoicedUndeliveredOrder_creditsAndCancels() throws Exception {
        UUID service = serviceProduct();
        JsonNode so = invoiceAll(confirm(createOrder(line(null, service, 8, 100))));

        JsonNode result = correction(so, "cancel", false, "{\"reason\":\"customer cancelled\"}")
                .andExpect(status().isOk()).json();

        assertThat(result.get("order").get("state").asText()).isEqualTo("CANCELLED");
        assertThat(netInvoiced(so)).isEqualByComparingTo("0");
        assertConsistent(so);
    }

    @Test
    void case41_cancelADeliveredAndInvoicedOrder_returnsCreditsAndCancels() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 3, 100)))));
        BigDecimal onHandBefore = onHand(product);

        JsonNode result = correction(so, "cancel", false, "{}").andExpect(status().isOk()).json();

        assertThat(result.get("order").get("state").asText()).isEqualTo("CANCELLED");
        assertThat(netInvoiced(so)).isEqualByComparingTo("0");
        assertThat(onHand(product)).isEqualByComparingTo(onHandBefore.add(new BigDecimal("3")));
        assertConsistent(so);
    }

    @Test
    void case42_reassignCustomerBeforeDelivery_movesTheInvoice() throws Exception {
        UUID service = serviceProduct();
        JsonNode so = invoiceAll(confirm(createOrder(line(null, service, 2, 100))));
        UUID other = createCustomer();

        JsonNode result = correction(so, "reassign-customer", false,
                "{\"customerPartnerId\":\"" + other + "\",\"reason\":\"wrong customer\"}")
                .andExpect(status().isOk()).json();

        assertThat(result.get("order").get("customerPartnerId").asText()).isEqualTo(other.toString());
        assertThat(netInvoiced(so)).isEqualByComparingTo("200");
        BigDecimal newCustomerInvoiced = BigDecimal.ZERO;
        for (JsonNode d : result.get("documents")) {
            if (!"CREDIT_NOTE".equals(d.path("moveType").asText())) {
                assertThat(d.get("customerPartnerId").asText()).isEqualTo(other.toString());
                newCustomerInvoiced = newCustomerInvoiced.add(d.get("amountTotal").decimalValue());
            }
        }
        assertThat(newCustomerInvoiced).isEqualByComparingTo("200");
        assertConsistent(so);
    }

    @Test
    void case42_reassignAfterDelivery_isRefused() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 2, 100)))));

        correction(so, "reassign-customer", false, "{\"customerPartnerId\":\"" + createCustomer() + "\"}")
                .refusedWith("already delivered");
        assertConsistent(so);
    }

    // ---------------------------------------------------------------- helpers

    private Result correction(JsonNode so, String flow, boolean preview, String body) throws Exception {
        return call(post(orderUrl(so) + "/corrections/" + flow).param("preview", String.valueOf(preview))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Posted invoices minus posted credit notes for the order, tax included. */
    private BigDecimal netInvoiced(JsonNode so) throws Exception {
        BigDecimal net = BigDecimal.ZERO;
        for (JsonNode inv : call(get("/api/v1/accounting/customer-invoices")).json()) {
            if (!so.get("id").asText().equals(inv.path("salesOrderId").asText())
                    || !"POSTED".equals(inv.get("state").asText())) {
                continue;
            }
            BigDecimal total = inv.get("amountTotal").decimalValue();
            net = "CREDIT_NOTE".equals(inv.path("moveType").asText()) ? net.subtract(total) : net.add(total);
        }
        return net;
    }

    private int documents(JsonNode so) throws Exception {
        int n = 0;
        for (JsonNode inv : call(get("/api/v1/accounting/customer-invoices")).json()) {
            if (so.get("id").asText().equals(inv.path("salesOrderId").asText())) {
                n++;
            }
        }
        return n;
    }

    private UUID saleTax() throws Exception {
        UUID account = null;
        for (JsonNode a : call(get("/api/v1/accounts").param("companyId", COMPANY_ID.toString())).json()) {
            if ("430011".equals(a.get("code").asText())) {
                account = UUID.fromString(a.get("id").asText());
            }
        }
        return call(post("/api/v1/purchase/fiscal-taxes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"P3-VAT-" + UUID.randomUUID().toString().substring(0, 6)
                        + "\",\"amountType\":\"PERCENT\",\"amount\":10,\"priceInclude\":false,"
                        + "\"scope\":\"SALE\",\"accountId\":\"" + account + "\"}"))
                .andExpect(status().isOk()).id();
    }
}
