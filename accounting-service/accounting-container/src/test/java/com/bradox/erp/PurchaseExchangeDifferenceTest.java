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

/** A bill in USD booked at one rate and paid at another: the difference goes to exchange gain or loss. */
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseExchangeDifferenceTest extends PurchaseScenarioSupport {

    private JsonNode billAt(String rate) throws Exception {
        String body = orderBody(vendor, null, line(null, product(), 5, 100))
                .replaceFirst("\\{", "{\"exchangeRateToCompany\":" + rate + ",");
        JsonNode po = call(post("/api/v1/purchase/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).json();
        return billAll(receiveAll(confirm(po)));
    }

    private Result payAt(UUID bill, String amount, String rate) throws Exception {
        return call(post("/api/v1/purchase/vendor-payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"vendorBillId\":\"" + bill + "\",\"bankJournalId\":\"" + bankJournal()
                        + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":" + amount
                        + ",\"currencyCode\":\"USD\",\"exchangeRateToCompany\":" + rate + "}"));
    }

    @Test
    void payingAtAHigherRate_isALoss_andPayableClears() throws Exception {
        JsonNode po = billAt("1500");
        UUID bill = postedBills(po).get(0);
        BigDecimal loss = accountBalance("430015");
        BigDecimal gain = accountBalance("430014");

        payAt(bill, "500", "1520").andExpect(status().isOk());

        // 500 USD booked at 1500 = 750,000; paid at 1520 = 760,000: 10,000 lost on the exchange.
        assertThat(accountBalance("430015").subtract(loss)).as("exchange loss").isEqualByComparingTo("10000");
        assertThat(accountBalance("430014").subtract(gain)).as("exchange gain").isEqualByComparingTo("0");
        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void payingAtALowerRate_isAGain() throws Exception {
        JsonNode po = billAt("1500");
        UUID bill = postedBills(po).get(0);
        BigDecimal gain = accountBalance("430014");
        BigDecimal loss = accountBalance("430015");

        payAt(bill, "500", "1480").andExpect(status().isOk());

        assertThat(accountBalance("430014").subtract(gain)).as("exchange gain").isEqualByComparingTo("-10000");
        assertThat(accountBalance("430015").subtract(loss)).as("exchange loss").isEqualByComparingTo("0");
        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
    }

    private BigDecimal fx() throws Exception {
        return accountBalance("430015").add(accountBalance("430014"));
    }

    /** Correcting a payment made at a different rate must not double or lose the exchange difference. */
    @Test
    void correctingAPaymentMadeAtAnotherRate_keepsTheSameExchangeLoss() throws Exception {
        JsonNode po = billAt("1500");
        UUID bill = postedBills(po).get(0);
        BigDecimal ap = accountBalance("430004");
        BigDecimal fxBefore = fx();
        JsonNode wrong = payAt(bill, "400", "1520").andExpect(status().isOk()).json();

        call(post("/api/v1/accounting/vendor-payments/" + wrong.get("id").asText() + "/correct")
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":500,\"reason\":\"typo\"}"))
                .andExpect(status().isOk());

        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        // The same 500 USD paid at 1520 as before: 10,000 lost, and the payable is clear.
        assertThat(fx().subtract(fxBefore)).isEqualByComparingTo("10000");
        // The 500 USD bill (750,000 at 1500) is cleared from the payable, once.
        assertThat(accountBalance("430004").subtract(ap)).as("payable cleared").isEqualByComparingTo("750000");
    }

    /** A paid bill that is credited and refunded: the whole cycle leaves nothing open on the payable. */
    @Test
    void paidThenCreditedThenRefunded_clearsThePayableExactlyOnce() throws Exception {
        JsonNode po = billAt("1500");
        UUID bill = postedBills(po).get(0);
        BigDecimal ap = accountBalance("430004");
        payAt(bill, "500", "1500").andExpect(status().isOk());
        call(post(orderUrl(po) + "/returns").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refund\":true,\"lines\":[{\"purchaseOrderLineId\":\"" + lineId(po, 0) + "\",\"qty\":2}]}"))
                .andExpect(status().isOk());

        JsonNode refund = call(post("/api/v1/accounting/vendor-bills/" + bill + "/credit/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bankJournalId\":\"" + bankJournal() + "\",\"paymentDate\":\"2026-05-05T12:00:00\"}"))
                .andExpect(status().isOk()).json();

        assertThat(refund.get("amount").decimalValue()).isEqualByComparingTo("200");
        assertThat(billDoc(bill).get("amountOverpaid").decimalValue()).isEqualByComparingTo("0");
        UUID creditNote = UUID.fromString(creditNotes(bill).get(0).get("id").asText());
        assertThat(billDoc(creditNote).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        // Payment clears the 750,000 bill; the credit note (300,000) and its refund cancel each other.
        assertThat(accountBalance("430004").subtract(ap)).isEqualByComparingTo("750000");
    }

    /** Credit kept and applied to the vendor's next bill, booked at a different rate. */
    @Test
    void keptCreditAppliedToABillAtAnotherRate_clearsBothBills() throws Exception {
        JsonNode first = billAt("1500");
        UUID bill = postedBills(first).get(0);
        UUID vendorId = UUID.fromString(first.get("vendorPartnerId").asText());
        payAt(bill, "500", "1500").andExpect(status().isOk());
        call(post(orderUrl(first) + "/returns").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refund\":true,\"lines\":[{\"purchaseOrderLineId\":\"" + lineId(first, 0) + "\",\"qty\":2}]}"))
                .andExpect(status().isOk());
        call(post("/api/v1/accounting/vendor-bills/" + bill + "/credit/keep")).andExpect(status().isOk());

        String body = orderBody(vendorId, null, line(null, product(), 2, 100))
                .replaceFirst("\\{", "{\"exchangeRateToCompany\":1520,");
        JsonNode second = billAll(receiveAll(confirm(call(post("/api/v1/purchase/orders")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).json())));
        UUID bill2 = postedBills(second).get(0);
        call(post("/api/v1/accounting/vendor-bills/" + bill2 + "/apply-credit")).andExpect(status().isOk());

        assertThat(billDoc(bill2).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertThat(billDoc(bill).get("amountOverpaid").decimalValue()).isEqualByComparingTo("0");
    }

    /** Two part payments at different rates: each part carries its own exchange difference. */
    @Test
    void partPaymentsAtDifferentRates_addUpTheirOwnDifferences() throws Exception {
        JsonNode po = billAt("1500");
        UUID bill = postedBills(po).get(0);
        BigDecimal ap = accountBalance("430004");
        BigDecimal fxBefore = fx();

        payAt(bill, "200", "1520").andExpect(status().isOk());   // 200 x 20 = 4,000 lost
        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("300");
        payAt(bill, "300", "1480").andExpect(status().isOk());   // 300 x 20 = 6,000 gained

        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertThat(fx().subtract(fxBefore)).as("net exchange result (loss minus gain)").isEqualByComparingTo("-2000");
        assertThat(accountBalance("430004").subtract(ap)).as("payable cleared").isEqualByComparingTo("750000");
    }

    /** Price variance on a foreign-currency bill is booked in the company currency and Stock Input still clears. */
    @Test
    void priceChangeOnAForeignCurrencyBill_booksTheVarianceInCompanyCurrency() throws Exception {
        BigDecimal stockInput = accountBalance("430011");
        BigDecimal variance = accountBalance("430026");
        JsonNode po = billAt("1500");

        call(post(orderUrl(po) + "/corrections/change-terms").param("preview", "false")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[{\"purchaseOrderLineId\":\"" + lineId(po, 0) + "\",\"unitPrice\":120}]}"))
                .andExpect(status().isOk());

        // 5 units cost 100 USD on receipt and are now billed at 120 USD: 100 USD x 1500 = 150,000.
        assertThat(netBilled(reload(po))).isEqualByComparingTo("600");
        assertThat(accountBalance("430026").subtract(variance)).as("variance").isEqualByComparingTo("150000");
        assertThat(accountBalance("430011")).as("stock input clear").isEqualByComparingTo(stockInput);
        assertConsistent(reload(po));
    }

    /** Variance on a multi-line bill: only the changed line carries it. */
    @Test
    void priceChangeOnOneLineOfAForeignCurrencyBill_leavesTheOtherLineAlone() throws Exception {
        BigDecimal stockInput = accountBalance("430011");
        BigDecimal variance = accountBalance("430026");
        String body = orderBody(vendor, null, line(null, product(), 2, 100), line(null, product(), 3, 50))
                .replaceFirst("\\{", "{\"exchangeRateToCompany\":1500,");
        JsonNode po = billAll(receiveAll(confirm(call(post("/api/v1/purchase/orders")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).json())));

        call(post(orderUrl(po) + "/corrections/change-terms").param("preview", "false")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[{\"purchaseOrderLineId\":\"" + lineId(po, 1) + "\",\"unitPrice\":60}]}"))
                .andExpect(status().isOk());

        // Second line: 3 x (60 - 50) = 30 USD x 1500 = 45,000. First line untouched.
        assertThat(accountBalance("430026").subtract(variance)).isEqualByComparingTo("45000");
        assertThat(accountBalance("430011")).isEqualByComparingTo(stockInput);
        assertThat(netBilled(reload(po))).isEqualByComparingTo("380");
        assertConsistent(reload(po));
    }
}
