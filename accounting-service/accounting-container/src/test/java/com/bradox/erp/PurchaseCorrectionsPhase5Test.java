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
 * Phase 5 for purchases: what happens to money when a paid or partly paid vendor bill is credited.
 * A credit note first reduces what we still owe; only the part we actually paid becomes vendor
 * credit, which the vendor refunds or which is kept for their next bill.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseCorrectionsPhase5Test extends PurchaseScenarioSupport {

    private JsonNode creditNoteOf(UUID bill) throws Exception {
        return creditNotes(bill).get(0);
    }

    private Result returnGoods(JsonNode po, String qty) throws Exception {
        return call(post(orderUrl(po) + "/returns").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refund\":true,\"lines\":[{\"purchaseOrderLineId\":\"" + lineId(po, 0) + "\",\"qty\":" + qty + "}]}"));
    }

    /** Money we paid a vendor that is not matched to any bill. */
    private BigDecimal vendorCredit(UUID vendorId) throws Exception {
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode p : call(get("/api/v1/purchase/vendor-payments")).json()) {
            if (vendorId.toString().equals(p.path("vendorPartnerId").asText())
                    && "POSTED".equals(p.path("state").asText())
                    && !"REFUND".equals(p.path("paymentKind").asText())) {
                total = total.add(p.path("unallocatedAmount").decimalValue());
            }
        }
        return total;
    }

    private JsonNode createOrderFor(UUID vendorId, Line... lines) throws Exception {
        return call(post("/api/v1/purchase/orders").contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(vendorId, null, lines))).andExpect(status().isOk()).json();
    }

    @Test
    void payingABillInFull_settlesIt() throws Exception {
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product(), 5, 100)))));
        UUID bill = postedBills(po).get(0);

        pay(bill, "500").andExpect(status().isOk());

        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(po);
    }

    /** A credit note on an unpaid bill cancels debt; the vendor cannot refund cash we never paid. */
    @Test
    void creditNoteOnAnUnpaidBill_cannotBeRefunded() throws Exception {
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product(), 5, 100)))));
        UUID bill = postedBills(po).get(0);
        returnGoods(po, "2").andExpect(status().isOk());
        UUID creditNote = UUID.fromString(creditNoteOf(bill).get("id").asText());

        pay(creditNote, "200").refusedWith("vendor credit");

        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("300");
        assertThat(billDoc(creditNote).get("amountRefundable").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(po);
    }

    @Test
    void partlyPaidBill_onlyTheOverpaidPartIsRefundable() throws Exception {
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product(), 5, 100)))));
        UUID bill = postedBills(po).get(0);
        pay(bill, "400").andExpect(status().isOk());
        returnGoods(po, "2").andExpect(status().isOk());
        UUID creditNote = UUID.fromString(creditNoteOf(bill).get("id").asText());

        // Owed after the credit: 500 - 200 = 300. Paid 400, so 100 is ours.
        assertThat(billDoc(bill).get("amountOverpaid").decimalValue()).isEqualByComparingTo("100");
        assertThat(billDoc(creditNote).get("amountRefundable").decimalValue()).isEqualByComparingTo("100");
        pay(creditNote, "150").refusedWith("vendor credit");
        pay(creditNote, "100").andExpect(status().isOk());

        assertThat(billDoc(creditNote).get("amountRefundable").decimalValue()).isEqualByComparingTo("0");
        assertThat(billDoc(bill).get("amountOverpaid").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(po);
    }

    @Test
    void fullyPaidBill_collectTheWholeCreditFromTheVendor() throws Exception {
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product(), 5, 100)))));
        UUID bill = postedBills(po).get(0);
        pay(bill, "500").andExpect(status().isOk());
        returnGoods(po, "2").andExpect(status().isOk());

        JsonNode refund = call(post("/api/v1/accounting/vendor-bills/" + bill + "/credit/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bankJournalId\":\"" + bankJournal() + "\",\"paymentDate\":\"2026-05-05T12:00:00\"}"))
                .andExpect(status().isOk()).json();

        assertThat(refund.get("paymentKind").asText()).isEqualTo("REFUND");
        assertThat(refund.get("amount").decimalValue()).isEqualByComparingTo("200");
        assertThat(billDoc(UUID.fromString(creditNoteOf(bill).get("id").asText())).get("amountResidual").decimalValue())
                .isEqualByComparingTo("0");
        assertConsistent(po);
    }

    @Test
    void keepTheCredit_thenApplyItToTheVendorsNextBill() throws Exception {
        UUID product = product();
        JsonNode first = billAll(receiveAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID bill = postedBills(first).get(0);
        UUID vendorId = UUID.fromString(first.get("vendorPartnerId").asText());
        pay(bill, "500").andExpect(status().isOk());
        returnGoods(first, "2").andExpect(status().isOk());

        JsonNode kept = call(post("/api/v1/accounting/vendor-bills/" + bill + "/credit/keep"))
                .andExpect(status().isOk()).json();

        assertThat(kept.get("keptAmount").decimalValue()).isEqualByComparingTo("200");
        assertThat(billDoc(bill).get("amountOverpaid").decimalValue()).isEqualByComparingTo("0");
        assertThat(vendorCredit(vendorId)).isEqualByComparingTo("200");

        JsonNode second = billAll(receiveAll(confirm(createOrderFor(vendorId, line(null, product, 3, 100)))));
        UUID bill2 = postedBills(second).get(0);
        JsonNode applied = call(post("/api/v1/accounting/vendor-bills/" + bill2 + "/apply-credit"))
                .andExpect(status().isOk()).json();

        assertThat(applied.get("appliedAmount").decimalValue()).isEqualByComparingTo("200");
        assertThat(billDoc(bill2).get("amountResidual").decimalValue()).isEqualByComparingTo("100");
        assertThat(vendorCredit(vendorId)).isEqualByComparingTo("0");
        assertConsistent(first);
        assertConsistent(second);
    }

    @Test
    void advancePayment_isAppliedWhenTheBillExists() throws Exception {
        UUID vendorId = createVendor();
        call(post("/api/v1/purchase/vendor-payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"vendorPartnerId\":\"" + vendorId + "\",\"bankJournalId\":\"" + bankJournal()
                        + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":300,\"currencyCode\":\"USD\","
                        + "\"reference\":\"ADVANCE\"}")).andExpect(status().isOk());
        assertThat(vendorCredit(vendorId)).isEqualByComparingTo("300");

        JsonNode po = billAll(receiveAll(confirm(createOrderFor(vendorId, line(null, product(), 5, 100)))));
        UUID bill = postedBills(po).get(0);
        call(post("/api/v1/accounting/vendor-bills/" + bill + "/apply-credit")).andExpect(status().isOk());

        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("200");
        assertThat(vendorCredit(vendorId)).isEqualByComparingTo("0");
        assertConsistent(po);
    }

    @Test
    void aWrongPaymentIsCorrectedInOneStep() throws Exception {
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product(), 5, 100)))));
        UUID bill = postedBills(po).get(0);
        JsonNode wrong = pay(bill, "400").andExpect(status().isOk()).json();

        JsonNode fixed = call(post("/api/v1/accounting/vendor-payments/" + wrong.get("id").asText() + "/correct")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":500,\"reason\":\"typo\"}")).andExpect(status().isOk()).json();

        assertThat(fixed.get("state").asText()).isEqualTo("POSTED");
        assertThat(fixed.get("amount").decimalValue()).isEqualByComparingTo("500");
        assertThat(fixed.get("id").asText()).isNotEqualTo(wrong.get("id").asText());
        assertThat(call(get("/api/v1/accounting/vendor-payments/" + wrong.get("id").asText())).json()
                .get("state").asText()).isEqualTo("REVERSED");
        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(po);
    }

    @Test
    void aPaymentMatchedToTheWrongBill_canBeMoved() throws Exception {
        UUID product = product();
        JsonNode first = billAll(receiveAll(confirm(createOrder(line(null, product, 2, 100)))));
        UUID vendorId = UUID.fromString(first.get("vendorPartnerId").asText());
        JsonNode second = billAll(receiveAll(confirm(createOrderFor(vendorId, line(null, product, 2, 100)))));
        UUID wrongBill = postedBills(first).get(0);
        UUID rightBill = postedBills(second).get(0);
        JsonNode payment = pay(wrongBill, "200").andExpect(status().isOk()).json();
        String allocationId = payment.get("allocations").get(0).get("id").asText();

        call(post("/api/v1/accounting/vendor-payments/allocations/" + allocationId + "/reverse"))
                .andExpect(status().isOk());
        call(post("/api/v1/accounting/vendor-payments/" + payment.get("id").asText() + "/allocations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"allocations\":[{\"billId\":\"" + rightBill + "\",\"amount\":200}]}"))
                .andExpect(status().isOk());

        assertThat(billDoc(wrongBill).get("amountResidual").decimalValue()).isEqualByComparingTo("200");
        assertThat(billDoc(rightBill).get("amountResidual").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(first);
        assertConsistent(second);
    }

    /** A price change after the bill was paid: our payment moves to the replacement bill. */
    @Test
    void priceChangeOnAPaidBill_movesThePaymentToTheNewBill() throws Exception {
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product(), 5, 100)))));
        UUID vendorId = UUID.fromString(po.get("vendorPartnerId").asText());
        pay(postedBills(po).get(0), "500").andExpect(status().isOk());

        call(post(orderUrl(po) + "/corrections/change-terms").param("preview", "false")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[{\"purchaseOrderLineId\":\"" + lineId(po, 0) + "\",\"unitPrice\":80}]}"))
                .andExpect(status().isOk());

        // The order is now worth 400 and we paid 500: 400 settles the new bill, 100 is ours.
        BigDecimal owed = BigDecimal.ZERO;
        for (UUID b : postedBills(po)) {
            owed = owed.add(billDoc(b).get("amountResidual").decimalValue());
        }
        assertThat(owed).isEqualByComparingTo("0");
        assertThat(vendorCredit(vendorId)).isEqualByComparingTo("100");
        assertConsistent(po);
    }

    @Test
    void cancellingAPaidOrderWithDocuments_leavesTheMoneyAsVendorCredit() throws Exception {
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product(), 3, 100)))));
        UUID vendorId = UUID.fromString(po.get("vendorPartnerId").asText());
        UUID bill = postedBills(po).get(0);
        pay(bill, "300").andExpect(status().isOk());

        call(post(orderUrl(po) + "/corrections/cancel").param("preview", "false")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());

        assertThat(billDoc(bill).get("amountOverpaid").decimalValue()).isEqualByComparingTo("300");
        call(post("/api/v1/accounting/vendor-bills/" + bill + "/credit/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bankJournalId\":\"" + bankJournal() + "\",\"paymentDate\":\"2026-05-05T12:00:00\"}"))
                .andExpect(status().isOk());
        assertThat(billDoc(bill).get("amountOverpaid").decimalValue()).isEqualByComparingTo("0");
        assertThat(vendorCredit(vendorId)).isEqualByComparingTo("0");
        assertConsistent(reload(po));
    }
}
