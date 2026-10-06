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
 * Phase 5 of the sales correction use cases: what happens to money when a paid or partly paid
 * invoice is credited. A credit note first reduces what the customer still owes; only the part the
 * customer has actually paid becomes customer credit, which is refunded or kept for the next invoice.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SalesCorrectionsPhase5Test extends SalesScenarioSupport {

    @Test
    void case47_payingAnInvoiceInFull_settlesIt() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID invoice = postedInvoices(so).get(0);

        pay(invoice, "500").andExpect(status().isOk());

        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(so);
    }

    /** A credit note on an unpaid invoice cancels debt; it must not be refundable in cash. */
    @Test
    void case49_creditNoteOnUnpaidInvoice_cannotBeRefundedInCash() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID invoice = postedInvoices(so).get(0);
        returnGoods(so, true, false, lineId(so, 0), "2").andExpect(status().isOk());
        UUID creditNote = UUID.fromString(creditNotes(invoice).get(0).get("id").asText());

        pay(creditNote, "200").refusedWith("customer credit");

        JsonNode inv = invoiceDoc(invoice);
        assertThat(inv.get("amountResidual").decimalValue()).isEqualByComparingTo("300");
        assertThat(invoiceDoc(creditNote).get("amountRefundable").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(so);
    }

    @Test
    void case49_partlyPaidInvoice_onlyTheOverpaidPartIsRefundable() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID invoice = postedInvoices(so).get(0);
        pay(invoice, "400").andExpect(status().isOk());
        returnGoods(so, true, false, lineId(so, 0), "2").andExpect(status().isOk());
        UUID creditNote = UUID.fromString(creditNotes(invoice).get(0).get("id").asText());

        // Owed after the credit: 500 - 200 = 300. Paid 400, so 100 is the customer's credit.
        assertThat(invoiceDoc(invoice).get("amountOverpaid").decimalValue()).isEqualByComparingTo("100");
        assertThat(invoiceDoc(creditNote).get("amountRefundable").decimalValue()).isEqualByComparingTo("100");
        pay(creditNote, "150").refusedWith("customer credit");
        pay(creditNote, "100").andExpect(status().isOk());

        assertThat(invoiceDoc(creditNote).get("amountRefundable").decimalValue()).isEqualByComparingTo("0");
        // The credit was refunded, so none is left with the customer on this invoice.
        assertThat(invoiceDoc(invoice).get("amountOverpaid").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(so);
    }

    @Test
    void case48_fullyPaidInvoice_refundTheWholeCredit() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID invoice = postedInvoices(so).get(0);
        pay(invoice, "500").andExpect(status().isOk());
        returnGoods(so, true, false, lineId(so, 0), "2").andExpect(status().isOk());

        JsonNode refund = call(post("/api/v1/accounting/customer-invoices/" + invoice + "/credit/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentJournalId\":\"" + cashJournal() + "\",\"paymentDate\":\"2026-05-05T12:00:00\"}"))
                .andExpect(status().isOk()).json();

        assertThat(refund.get("paymentKind").asText()).isEqualTo("REFUND");
        assertThat(refund.get("amount").decimalValue()).isEqualByComparingTo("200");
        UUID creditNote = UUID.fromString(creditNotes(invoice).get(0).get("id").asText());
        assertThat(invoiceDoc(creditNote).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(so);
    }

    @Test
    void case48_keepTheCredit_thenApplyItToTheNextInvoice() throws Exception {
        UUID product = stockedProduct(40);
        JsonNode first = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID invoice = postedInvoices(first).get(0);
        UUID customerId = UUID.fromString(first.get("customerPartnerId").asText());
        pay(invoice, "500").andExpect(status().isOk());
        returnGoods(first, true, false, lineId(first, 0), "2").andExpect(status().isOk());

        JsonNode kept = call(post("/api/v1/accounting/customer-invoices/" + invoice + "/credit/keep"))
                .andExpect(status().isOk()).json();

        assertThat(kept.get("keptAmount").decimalValue()).isEqualByComparingTo("200");
        assertThat(invoiceDoc(invoice).get("amountOverpaid").decimalValue()).isEqualByComparingTo("0");
        assertThat(customerCredit(customerId)).isEqualByComparingTo("200");

        // The customer's next order, for the same customer, is covered by the credit that was kept.
        JsonNode second = invoiceAll(deliverAll(confirm(createOrderFor(customerId, line(null, product, 3, 100)))));
        UUID invoice2 = postedInvoices(second).get(0);
        JsonNode applied = call(post("/api/v1/accounting/customer-invoices/" + invoice2 + "/apply-credit"))
                .andExpect(status().isOk()).json();

        assertThat(applied.get("appliedAmount").decimalValue()).isEqualByComparingTo("200");
        assertThat(invoiceDoc(invoice2).get("amountResidual").decimalValue()).isEqualByComparingTo("100");
        assertThat(customerCredit(customerId)).isEqualByComparingTo("0");
        assertConsistent(first);
        assertConsistent(second);
    }

    @Test
    void case50_advancePayment_isAppliedWhenTheInvoiceExists() throws Exception {
        UUID product = stockedProduct(20);
        UUID customerId = createCustomer();
        // Money arrives before any invoice: a payment for the customer with no document.
        call(post("/api/v1/accounting/customer-invoices/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerPartnerId\":\"" + customerId + "\",\"paymentJournalId\":\"" + cashJournal()
                        + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":300,\"currencyCode\":\"USD\","
                        + "\"reference\":\"ADVANCE\"}")).andExpect(status().isOk());
        assertThat(customerCredit(customerId)).isEqualByComparingTo("300");

        JsonNode so = invoiceAll(deliverAll(confirm(createOrderFor(customerId, line(null, product, 5, 100)))));
        UUID invoice = postedInvoices(so).get(0);
        call(post("/api/v1/accounting/customer-invoices/" + invoice + "/apply-credit")).andExpect(status().isOk());

        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("200");
        assertThat(customerCredit(customerId)).isEqualByComparingTo("0");
        assertConsistent(so);
    }

    @Test
    void case51_overpayment_staysAsCustomerCredit() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID invoice = postedInvoices(so).get(0);
        UUID customerId = UUID.fromString(so.get("customerPartnerId").asText());

        // 600 received for a 500 invoice: 500 settles the invoice, 100 stays with the customer.
        call(post("/api/v1/accounting/customer-invoices/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerPartnerId\":\"" + customerId + "\",\"paymentJournalId\":\"" + cashJournal()
                        + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":600,\"currencyCode\":\"USD\","
                        + "\"allocations\":[{\"invoiceId\":\"" + invoice + "\",\"amount\":500}]}"))
                .andExpect(status().isOk());

        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertThat(customerCredit(customerId)).isEqualByComparingTo("100");
        assertConsistent(so);
    }

    @Test
    void case52_aWrongPaymentIsCorrectedInOneStep() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID invoice = postedInvoices(so).get(0);
        JsonNode wrong = pay(invoice, "400").andExpect(status().isOk()).json();

        JsonNode fixed = call(post("/api/v1/accounting/customer-payments/" + wrong.get("id").asText() + "/correct")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":500,\"reason\":\"typo\"}")).andExpect(status().isOk()).json();

        assertThat(fixed.get("state").asText()).isEqualTo("POSTED");
        assertThat(fixed.get("amount").decimalValue()).isEqualByComparingTo("500");
        assertThat(fixed.get("id").asText()).isNotEqualTo(wrong.get("id").asText());
        assertThat(call(get("/api/v1/accounting/customer-payments/" + wrong.get("id").asText())).json()
                .get("state").asText()).isEqualTo("REVERSED");
        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(so);
    }

    @Test
    void case53_aPaymentMatchedToTheWrongInvoice_canBeMoved() throws Exception {
        UUID product = stockedProduct(40);
        JsonNode first = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 2, 100)))));
        UUID customerId = UUID.fromString(first.get("customerPartnerId").asText());
        JsonNode second = invoiceAll(deliverAll(confirm(createOrderFor(customerId, line(null, product, 2, 100)))));
        UUID wrongInvoice = postedInvoices(first).get(0);
        UUID rightInvoice = postedInvoices(second).get(0);
        JsonNode payment = pay(wrongInvoice, "200").andExpect(status().isOk()).json();
        String allocationId = payment.get("allocations").get(0).get("id").asText();

        call(post("/api/v1/accounting/customer-payments/allocations/" + allocationId + "/reverse"))
                .andExpect(status().isOk());
        call(post("/api/v1/accounting/customer-payments/" + payment.get("id").asText() + "/allocations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"allocations\":[{\"invoiceId\":\"" + rightInvoice + "\",\"amount\":200}]}"))
                .andExpect(status().isOk());

        assertThat(invoiceDoc(wrongInvoice).get("amountResidual").decimalValue()).isEqualByComparingTo("200");
        assertThat(invoiceDoc(rightInvoice).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(first);
        assertConsistent(second);
    }

    /** A price change after the invoice was paid: the customer's money moves to the new invoice. */
    @Test
    void case33_priceChangeOnAPaidInvoice_movesThePaymentToTheNewInvoice() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID customerId = UUID.fromString(so.get("customerPartnerId").asText());
        pay(postedInvoices(so).get(0), "500").andExpect(status().isOk());

        call(post(orderUrl(so) + "/corrections/change-terms").param("preview", "false")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[{\"salesOrderLineId\":\"" + lineId(so, 0) + "\",\"unitPrice\":80}]}"))
                .andExpect(status().isOk());

        // The order is now worth 400 and the customer paid 500: 400 settles the new invoice, 100 is theirs.
        BigDecimal owed = BigDecimal.ZERO;
        for (UUID inv : postedInvoices(so)) {
            owed = owed.add(invoiceDoc(inv).get("amountResidual").decimalValue());
        }
        assertThat(owed).isEqualByComparingTo("0");
        assertThat(customerCredit(customerId)).isEqualByComparingTo("100");
        assertConsistent(so);
    }

    /** Case 55: a foreign-currency invoice is credited at the invoice's own exchange rate. */
    @Test
    void case55_creditNoteOfAForeignCurrencyInvoice_usesTheInvoicesRate() throws Exception {
        UUID product = stockedProduct(20);
        String body = orderBody(customer, null, line(null, product, 4, 1310)).replace("\"currencyCode\":\"USD\"", "\"currencyCode\":\"IQD\"");
        JsonNode so = invoiceAll(deliverAll(confirm(call(post("/api/v1/sales/orders")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).json())));
        UUID invoice = postedInvoices(so).get(0);
        JsonNode invoiceBefore = invoiceDoc(invoice);

        returnGoods(so, true, false, lineId(so, 0), "1").andExpect(status().isOk());

        JsonNode creditNote = creditNotes(invoice).get(0);
        assertThat(creditNote.get("currencyCode").asText()).isEqualTo("IQD");
        assertThat(creditNote.get("exchangeRateToCompany").decimalValue())
                .isEqualByComparingTo(invoiceBefore.get("exchangeRateToCompany").decimalValue());
        assertThat(creditNote.get("amountTotal").decimalValue()).isEqualByComparingTo("1310");
        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("3930");
        assertConsistent(so);
    }

    // ---------------------------------------------------------------- helpers

    private JsonNode createOrderFor(UUID customerId, Line... lines) throws Exception {
        return call(post("/api/v1/sales/orders").contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(customerId, null, lines))).andExpect(status().isOk()).json();
    }

    /** Money the customer has paid that is not matched to any invoice. */
    private BigDecimal customerCredit(UUID customerId) throws Exception {
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode p : call(get("/api/v1/accounting/customer-invoices/payments")).json()) {
            if (customerId.toString().equals(p.path("customerPartnerId").asText())
                    && "POSTED".equals(p.path("state").asText())
                    && !"REFUND".equals(p.path("paymentKind").asText())) {
                total = total.add(p.path("unallocatedAmount").decimalValue());
            }
        }
        return total;
    }
}
