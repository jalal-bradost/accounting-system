package com.bradox.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A receipt in one currency settling an invoice in another. In the test company USD is the base and
 * IQD the foreign currency: the invoice is 10,000 IQD booked at 0.0007 (= 7 USD), received in USD.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerCrossCurrencyPaymentTest extends SalesScenarioSupport {

    private UUID iqdInvoice() throws Exception {
        UUID product = stockedProduct(10);
        String body = orderBody(customer, null, line(null, product, 1, 10000))
                .replace("\"currencyCode\":\"USD\"", "\"currencyCode\":\"IQD\"")
                .replaceFirst("\\{", "{\"exchangeRateToCompany\":0.0007,");
        JsonNode so = call(post("/api/v1/sales/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).json();
        so = invoiceAll(deliverAll(confirm(so)));
        return postedInvoices(so).get(0);
    }

    private Result receiveUsd(UUID invoice, String amount) throws Exception {
        return call(post("/api/v1/accounting/customer-invoices/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerInvoiceId\":\"" + invoice + "\",\"paymentJournalId\":\"" + cashJournal()
                        + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":" + amount
                        + ",\"currencyCode\":\"USD\",\"exchangeRateToCompany\":1}"));
    }

    @Test
    void receivingTheInvoiceInTheOtherCurrency_settlesItAtTheInvoicesRate() throws Exception {
        UUID invoice = iqdInvoice();
        assertThat(invoiceDoc(invoice).get("currencyCode").asText()).isEqualTo("IQD");

        JsonNode payment = receiveUsd(invoice, "7").andExpect(status().isOk()).json();

        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertThat(payment.get("unallocatedAmount").decimalValue()).isEqualByComparingTo("0");
        JsonNode alloc = payment.get("allocations").get(0);
        assertThat(alloc.get("amount").decimalValue()).as("document currency (IQD)").isEqualByComparingTo("10000");
        assertThat(alloc.get("paymentAmount").decimalValue()).as("payment currency (USD)").isEqualByComparingTo("7");
    }

    @Test
    void receivingMoreThanOwed_leavesTheRestAsCustomerCredit() throws Exception {
        UUID invoice = iqdInvoice();
        JsonNode payment = receiveUsd(invoice, "7.5").andExpect(status().isOk()).json();
        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertThat(payment.get("unallocatedAmount").decimalValue()).isEqualByComparingTo("0.5");
    }

    @Test
    void aPartReceiptInTheOtherCurrency_reducesTheInvoiceProportionally() throws Exception {
        UUID invoice = iqdInvoice();
        receiveUsd(invoice, "3.5").andExpect(status().isOk());
        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("5000");
        receiveUsd(invoice, "3.5").andExpect(status().isOk());
        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void correctingACrossCurrencyReceipt_keepsTheInvoiceSettledCorrectly() throws Exception {
        UUID invoice = iqdInvoice();
        JsonNode wrong = receiveUsd(invoice, "3.5").andExpect(status().isOk()).json();

        call(post("/api/v1/accounting/customer-payments/" + wrong.get("id").asText() + "/correct")
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":7,\"reason\":\"typo\"}"))
                .andExpect(status().isOk());

        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
    }

    /** Money received in USD with no invoice, applied later to an IQD invoice. */
    @Test
    void anAdvanceInTheOtherCurrency_canBeAppliedToTheInvoice() throws Exception {
        UUID invoice = iqdInvoice();
        UUID customerId = UUID.fromString(invoiceDoc(invoice).get("customerPartnerId").asText());
        call(post("/api/v1/accounting/customer-invoices/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerPartnerId\":\"" + customerId + "\",\"paymentJournalId\":\"" + cashJournal()
                        + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":10,\"currencyCode\":\"USD\","
                        + "\"exchangeRateToCompany\":1}")).andExpect(status().isOk());

        JsonNode applied = call(post("/api/v1/accounting/customer-invoices/" + invoice + "/apply-credit"))
                .andExpect(status().isOk()).json();

        // 10 USD covers the 7 USD (10,000 IQD) the invoice owes; 3 USD stay with the customer.
        assertThat(applied.get("appliedAmount").decimalValue()).isEqualByComparingTo("10000");
        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
    }
}
