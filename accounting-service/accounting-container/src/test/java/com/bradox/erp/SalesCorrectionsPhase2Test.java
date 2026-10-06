package com.bradox.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 2 of the sales correction use cases: returns and their credit notes happen in one step.
 * A refund return lowers the order and posts the credit note with the return; a replacement return
 * re-delivers; damaged goods are scrapped; returns are valued at the original delivery cost.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SalesCorrectionsPhase2Test extends SalesScenarioSupport {

    @Test
    void case22_returnBeforeInvoicing_refund_lowersTheOrderWithoutCreditNote() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = deliverAll(confirm(createOrder(line(null, product, 5, 100))));
        BigDecimal onHandBefore = onHand(product);

        so = returnGoods(so, true, false, lineId(so, 0), "2").andExpect(status().isOk()).json();

        JsonNode l = so.get("lines").get(0);
        assertThat(l.get("qtyOrdered").decimalValue()).isEqualByComparingTo("3");
        assertThat(l.get("qtyDelivered").decimalValue()).isEqualByComparingTo("3");
        assertThat(l.get("qtyInvoiced").decimalValue()).isEqualByComparingTo("0");
        assertThat(openDeliveries(so)).isEmpty();
        assertThat(onHand(product)).isEqualByComparingTo(onHandBefore.add(new BigDecimal("2")));
        assertConsistent(so);
    }

    @Test
    void case40_returnAfterInvoicing_refund_postsTheCreditNoteInTheSameStep() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID invoice = postedInvoices(so).get(0);

        so = returnGoods(so, true, false, lineId(so, 0), "2").andExpect(status().isOk()).json();

        JsonNode l = so.get("lines").get(0);
        assertThat(l.get("qtyOrdered").decimalValue()).isEqualByComparingTo("3");
        assertThat(l.get("qtyDelivered").decimalValue()).isEqualByComparingTo("3");
        assertThat(l.get("qtyInvoiced").decimalValue()).isEqualByComparingTo("3");
        JsonNode cns = creditNotes(invoice);
        assertThat(cns).hasSize(1);
        assertThat(cns.get(0).get("state").asText()).isEqualTo("POSTED");
        assertThat(cns.get(0).get("lines").get(0).get("qty").decimalValue()).isEqualByComparingTo("2");
        assertThat(cns.get(0).get("lines").get(0).get("unitPrice").decimalValue()).isEqualByComparingTo("100");
        JsonNode inv = call(get("/api/v1/accounting/customer-invoices/" + invoice)).andExpect(status().isOk()).json();
        assertThat(inv.get("amountCredited").decimalValue()).isEqualByComparingTo("200");
        assertThat(inv.get("amountResidual").decimalValue()).isEqualByComparingTo("300");
        assertConsistent(so);
    }

    /** The classic flow (draft return from the order, validated on the picking page) is atomic too. */
    @Test
    void case40_validatingAReturnOnThePickingPage_alsoPostsTheCreditNote() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 4, 100)))));
        UUID invoice = postedInvoices(so).get(0);

        UUID ret = call(post(orderUrl(so) + "/return").contentType(MediaType.APPLICATION_JSON)
                .content("{\"toRefund\":true}")).andExpect(status().isOk()).id();
        validate(ret, firstMoveId(ret), "1", false);

        JsonNode cns = creditNotes(invoice);
        assertThat(cns).hasSize(1);
        assertThat(cns.get(0).get("state").asText()).isEqualTo("POSTED");
        assertThat(reload(so).get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("3");
        assertConsistent(so);
    }

    @Test
    void case44_returnForReplacement_redeliversWithoutCreditNote() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID invoice = postedInvoices(so).get(0);

        so = returnGoods(so, false, false, lineId(so, 0), "2").andExpect(status().isOk()).json();

        JsonNode l = so.get("lines").get(0);
        assertThat(l.get("qtyOrdered").decimalValue()).isEqualByComparingTo("5");
        assertThat(l.get("qtyDelivered").decimalValue()).isEqualByComparingTo("3");
        assertThat(l.get("qtyInvoiced").decimalValue()).isEqualByComparingTo("5");
        assertThat(creditNotes(invoice)).isEmpty();
        assertThat(openDeliveries(so)).hasSize(1);
        assertThat(openDemand(so, lineId(so, 0))).isEqualByComparingTo("2");

        so = deliverAll(so);
        assertThat(so.get("lines").get(0).get("qtyDelivered").decimalValue()).isEqualByComparingTo("5");
        assertConsistent(so);
    }

    @Test
    void case45_damagedReturn_isCreditedButNotRestocked() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID invoice = postedInvoices(so).get(0);
        BigDecimal onHandBefore = onHand(product);

        so = returnGoods(so, true, true, lineId(so, 0), "2").andExpect(status().isOk()).json();

        assertThat(onHand(product)).isEqualByComparingTo(onHandBefore);
        assertThat(creditNotes(invoice)).hasSize(1);
        assertThat(so.get("lines").get(0).get("qtyDelivered").decimalValue()).isEqualByComparingTo("3");
        assertConsistent(so);
    }

    @Test
    void case46_returnIsValuedAtTheOriginalDeliveryCost() throws Exception {
        UUID product = stockedProduct(10);
        JsonNode so = deliverAll(confirm(createOrder(line(null, product, 4, 100))));
        receive(product, 10, 40);

        returnGoods(so, true, false, lineId(so, 0), "1").andExpect(status().isOk());

        JsonNode layers = call(get("/api/v1/inventory/valuation-layers").param("productId", product.toString()))
                .andExpect(status().isOk()).json();
        JsonNode returnLayer = null;
        for (JsonNode layer : layers) {
            if (layer.get("quantity").decimalValue().compareTo(BigDecimal.ONE) == 0) {
                returnLayer = layer;
            }
        }
        assertThat(returnLayer).as("valuation layer of the returned unit").isNotNull();
        assertThat(returnLayer.get("unitCost").decimalValue()).isEqualByComparingTo("10");
        assertConsistent(so);
    }

    @Test
    void returnSpanningTwoDeliveriesAndTwoInvoices_isSplitAcrossBoth() throws Exception {
        UUID product = stockedProduct(20);
        // Two deliveries and two invoices: 3 delivered + invoiced, then the order grows to 5 and the
        // extra 2 are delivered + invoiced separately.
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 3, 100)))));
        so = amend(so, line(lineId(so, 0), product, 5, 100)).andExpect(status().isOk()).json();
        so = invoiceAll(deliverAll(so));
        assertThat(postedInvoices(so)).hasSize(2);
        assertThat(so.get("deliveryPickingIds")).hasSize(2);

        so = returnGoods(so, true, false, lineId(so, 0), "4").andExpect(status().isOk()).json();

        JsonNode l = so.get("lines").get(0);
        assertThat(l.get("qtyDelivered").decimalValue()).isEqualByComparingTo("1");
        assertThat(l.get("qtyInvoiced").decimalValue()).isEqualByComparingTo("1");
        assertThat(l.get("qtyOrdered").decimalValue()).isEqualByComparingTo("1");
        // However the return was split over the deliveries, exactly 4 units are credited, all posted,
        // and neither invoice is credited beyond what it billed.
        BigDecimal credited = BigDecimal.ZERO;
        for (UUID inv : postedInvoices(so)) {
            BigDecimal perInvoice = BigDecimal.ZERO;
            for (JsonNode cn : creditNotes(inv)) {
                assertThat(cn.get("state").asText()).isEqualTo("POSTED");
                for (JsonNode cl : cn.get("lines")) {
                    perInvoice = perInvoice.add(cl.get("qty").decimalValue());
                }
            }
            JsonNode invDoc = call(get("/api/v1/accounting/customer-invoices/" + inv)).json();
            BigDecimal billed = BigDecimal.ZERO;
            for (JsonNode il : invDoc.get("lines")) {
                billed = billed.add(il.get("qty").decimalValue());
            }
            assertThat(perInvoice).isLessThanOrEqualTo(billed);
            credited = credited.add(perInvoice);
        }
        assertThat(credited).isEqualByComparingTo("4");
        assertConsistent(so);
    }

    /** Returning one line of a multi-line delivery must not return the other lines. */
    @Test
    void returningOneLineOfAMultiLineDelivery_leavesTheOtherLinesDelivered() throws Exception {
        UUID a = stockedProduct(20);
        UUID b = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, a, 2, 100), line(null, b, 3, 50)))));
        BigDecimal onHandB = onHand(b);

        so = returnGoods(so, true, false, lineId(so, 0), "1").andExpect(status().isOk()).json();

        assertThat(so.get("lines").get(0).get("qtyDelivered").decimalValue()).isEqualByComparingTo("1");
        assertThat(so.get("lines").get(1).get("qtyDelivered").decimalValue()).isEqualByComparingTo("3");
        assertThat(so.get("lines").get(1).get("qtyOrdered").decimalValue()).isEqualByComparingTo("3");
        assertThat(onHand(b)).isEqualByComparingTo(onHandB);
        assertConsistent(so);
    }

    @Test
    void ruleR7_returningMoreThanDelivered_isRefusedAndChangesNothing() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = deliverAll(confirm(createOrder(line(null, product, 3, 100))));
        long version = so.get("rowVersion").asLong();

        returnGoods(so, true, false, lineId(so, 0), "4").refusedWith("more than was delivered");

        assertThat(reload(so).get("rowVersion").asLong()).isEqualTo(version);
        assertThat(reload(so).get("returnPickingIds")).isEmpty();
        assertConsistent(so);
    }

    // ---------------------------------------------------------------- helpers


}
