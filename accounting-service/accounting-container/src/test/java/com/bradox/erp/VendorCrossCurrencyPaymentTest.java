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
 * A payment in one currency settling a bill in another. In the test company USD is the base and
 * IQD the foreign currency: the bill is 10,000 IQD booked at 0.0007 (= 7 USD) and is paid in USD.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VendorCrossCurrencyPaymentTest extends PurchaseScenarioSupport {

    private JsonNode iqdBill() throws Exception {
        String body = orderBody(vendor, null, line(null, product(), 1, 10000))
                .replace("\"currencyCode\":\"USD\"", "\"currencyCode\":\"IQD\"")
                .replaceFirst("\\{", "{\"exchangeRateToCompany\":0.0007,");
        JsonNode po = call(post("/api/v1/purchase/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).json();
        return billAll(receiveAll(confirm(po)));
    }

    private Result payUsd(UUID bill, String amount) throws Exception {
        return call(post("/api/v1/purchase/vendor-payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"vendorBillId\":\"" + bill + "\",\"bankJournalId\":\"" + bankJournal()
                        + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":" + amount
                        + ",\"currencyCode\":\"USD\",\"exchangeRateToCompany\":1}"));
    }

    @Test
    void payingTheBillInTheOtherCurrency_settlesItAtTheBillsRate() throws Exception {
        JsonNode po = iqdBill();
        UUID bill = postedBills(po).get(0);
        assertThat(billDoc(bill).get("currencyCode").asText()).isEqualTo("IQD");

        JsonNode payment = payUsd(bill, "7").andExpect(status().isOk()).json();

        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertThat(payment.get("unallocatedAmount").decimalValue()).isEqualByComparingTo("0");
        JsonNode alloc = payment.get("allocations").get(0);
        assertThat(alloc.get("amount").decimalValue()).as("document currency (IQD)").isEqualByComparingTo("10000");
        assertThat(alloc.get("paymentAmount").decimalValue()).as("payment currency (USD)").isEqualByComparingTo("7");
        assertConsistent(po);
    }

    @Test
    void payingMoreThanTheBillOwes_leavesTheRestAsVendorCredit() throws Exception {
        JsonNode po = iqdBill();
        UUID bill = postedBills(po).get(0);

        JsonNode payment = payUsd(bill, "7.5").andExpect(status().isOk()).json();

        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertThat(payment.get("unallocatedAmount").decimalValue()).isEqualByComparingTo("0.5");
    }

    @Test
    void aPartPaymentInTheOtherCurrency_reducesTheBillProportionally() throws Exception {
        JsonNode po = iqdBill();
        UUID bill = postedBills(po).get(0);

        payUsd(bill, "3.5").andExpect(status().isOk());
        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("5000");

        payUsd(bill, "3.5").andExpect(status().isOk());
        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void anExplicitPaymentAmount_booksTheDifferenceAsExchangeLoss() throws Exception {
        JsonNode po = iqdBill();
        UUID bill = postedBills(po).get(0);
        BigDecimal loss = accountBalance("430015");
        // Registered with no document, then matched: 10,000 IQD settled using 7.2 USD.
        JsonNode payment = call(post("/api/v1/purchase/vendor-payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"vendorPartnerId\":\"" + po.get("vendorPartnerId").asText() + "\",\"bankJournalId\":\""
                        + bankJournal() + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":7.2,"
                        + "\"currencyCode\":\"USD\",\"exchangeRateToCompany\":1}")).andExpect(status().isOk()).json();

        call(post("/api/v1/accounting/vendor-payments/" + payment.get("id").asText() + "/allocations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"allocations\":[{\"billId\":\"" + bill + "\",\"amount\":10000,\"paymentAmount\":7.2}]}"))
                .andExpect(status().isOk());

        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertThat(accountBalance("430015").subtract(loss)).as("0.2 USD paid over the 7 USD booked")
                .isEqualByComparingTo("0.2");
    }

    @Test
    void correctingACrossCurrencyPayment_keepsTheBillSettledCorrectly() throws Exception {
        JsonNode po = iqdBill();
        UUID bill = postedBills(po).get(0);
        JsonNode wrong = payUsd(bill, "3.5").andExpect(status().isOk()).json();

        call(post("/api/v1/accounting/vendor-payments/" + wrong.get("id").asText() + "/correct")
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":7,\"reason\":\"typo\"}"))
                .andExpect(status().isOk());

        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(po);
    }
}
