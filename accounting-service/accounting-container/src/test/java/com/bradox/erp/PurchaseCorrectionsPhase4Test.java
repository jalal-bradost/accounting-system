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
 * Phase 4 for purchases: reduce quantities after billing, cancel an order together with its
 * documents, and move an order to the right vendor before anything was received.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseCorrectionsPhase4Test extends PurchaseScenarioSupport {

    private Result correction(JsonNode po, String action, boolean preview, String body) throws Exception {
        return call(post(orderUrl(po) + "/corrections/" + action).param("preview", String.valueOf(preview))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String qty(JsonNode po, int index, String qty) {
        return "{\"lines\":[{\"purchaseOrderLineId\":\"" + lineId(po, index) + "\",\"qty\":" + qty + "}]}";
    }

    // ---------------------------------------------------------------- reduce quantities

    @Test
    void reduceBilledServiceLine_creditsTheDifference() throws Exception {
        UUID service = serviceProduct();
        JsonNode po = billAll(confirm(createOrder(line(null, service, 3, 100))));
        UUID bill = postedBills(po).get(0);

        JsonNode result = correction(po, "reduce-quantities", false, qty(po, 0, "1")).andExpect(status().isOk()).json();

        assertThat(result.get("documents")).hasSize(1);
        assertThat(creditNotes(bill).get(0).get("amountTotal").decimalValue()).isEqualByComparingTo("200");
        po = reload(po);
        assertThat(po.get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("1");
        assertThat(netBilled(po)).isEqualByComparingTo("100");
        assertConsistent(po);
    }

    @Test
    void reduceToWhatWasReceived_cancelsTheOpenReceiptAndNeedsNoCredit() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 5, 100)));
        UUID receipt = openReceipts(po).get(0);
        validate(receipt, firstMoveId(receipt), "3", true);
        po = billAll(reload(po));
        assertThat(openReceipts(po)).hasSize(1);

        JsonNode result = correction(po, "reduce-quantities", false, qty(po, 0, "3")).andExpect(status().isOk()).json();

        assertThat(result.get("documents")).isEmpty();
        po = reload(po);
        assertThat(po.get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("3");
        assertThat(openReceipts(po)).isEmpty();
        assertThat(netBilled(po)).isEqualByComparingTo("300");
        assertConsistent(po);
    }

    @Test
    void reduceBelowReceived_isRefusedAndAsksForAReturn() throws Exception {
        UUID product = product();
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product, 5, 100)))));

        correction(po, "reduce-quantities", false, qty(po, 0, "3")).refusedWith("below the received quantity");

        assertThat(netBilled(reload(po))).isEqualByComparingTo("500");
    }

    @Test
    void reduceCannotRaiseAQuantity() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 5, 100)));

        correction(po, "reduce-quantities", false, qty(po, 0, "9")).refusedWith("only lower");
    }

    // ---------------------------------------------------------------- cancel with documents

    @Test
    void cancelWithDocuments_returnsCreditsAndCancelsInOneStep() throws Exception {
        UUID product = product();
        UUID service = serviceProduct();
        BigDecimal onHandBefore = onHand(product);
        BigDecimal stockInput = accountBalance("430011");
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product, 4, 100), line(null, service, 2, 50)))));
        assertThat(netBilled(po)).isEqualByComparingTo("500");

        JsonNode result = correction(po, "cancel", false, "{\"reason\":\"ordered by mistake\"}")
                .andExpect(status().isOk()).json();

        assertThat(result.get("order").get("state").asText()).isEqualTo("CANCELLED");
        assertThat(result.get("documents")).isNotEmpty();
        po = reload(po);
        assertThat(netBilled(po)).isEqualByComparingTo("0");
        assertThat(onHand(product)).isEqualByComparingTo(onHandBefore);
        assertThat(accountBalance("430011")).isEqualByComparingTo(stockInput);
        assertConsistent(po);
    }

    @Test
    void cancelWithDocuments_preview_changesNothing() throws Exception {
        UUID product = product();
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product, 3, 100)))));
        BigDecimal onHand = onHand(product);

        JsonNode preview = correction(po, "cancel", true, "{}").andExpect(status().isOk()).json();

        assertThat(preview.get("preview").asBoolean()).isTrue();
        assertThat(preview.get("documents")).isNotEmpty();
        po = reload(po);
        assertThat(po.get("state").asText()).isEqualTo("CONFIRMED");
        assertThat(netBilled(po)).isEqualByComparingTo("300");
        assertThat(onHand(product)).isEqualByComparingTo(onHand);
        assertConsistent(po);
    }

    @Test
    void cancelWithDocuments_ofAnUntouchedOrder_justCancels() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 3, 100)));

        JsonNode result = correction(po, "cancel", false, "{}").andExpect(status().isOk()).json();

        assertThat(result.get("order").get("state").asText()).isEqualTo("CANCELLED");
        assertThat(result.get("documents")).isEmpty();
        assertThat(openReceipts(reload(po))).isEmpty();
    }

    // ---------------------------------------------------------------- reassign vendor

    @Test
    void reassignVendor_beforeReceipt_creditsTheOldVendorAndBillsTheNewOne() throws Exception {
        UUID service = serviceProduct();
        JsonNode po = billAll(confirm(createOrder(line(null, service, 2, 100))));
        UUID newVendor = createVendor();

        JsonNode result = correction(po, "reassign-vendor", false,
                "{\"vendorPartnerId\":\"" + newVendor + "\",\"reason\":\"wrong vendor\"}")
                .andExpect(status().isOk()).json();

        assertThat(result.get("documents")).hasSize(2);
        po = reload(po);
        assertThat(po.get("vendorPartnerId").asText()).isEqualTo(newVendor.toString());
        assertThat(netBilled(po)).isEqualByComparingTo("200");
        UUID rebill = postedBills(po).stream()
                .filter(b -> newVendor.toString().equals(billDocVendor(b)))
                .findFirst().orElseThrow();
        assertThat(billDoc(rebill).get("amountTotal").decimalValue()).isEqualByComparingTo("200");
        assertConsistent(po);
    }

    @Test
    void reassignVendor_movesTheOpenReceiptToTheNewVendor() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 3, 100)));
        UUID newVendor = createVendor();

        correction(po, "reassign-vendor", false, "{\"vendorPartnerId\":\"" + newVendor + "\"}")
                .andExpect(status().isOk());

        po = reload(po);
        assertThat(openReceipts(po)).hasSize(1);
        assertThat(picking(openReceipts(po).get(0)).get("partnerId").asText()).isEqualTo(newVendor.toString());
        assertConsistent(po);
    }

    @Test
    void reassignVendor_afterReceipt_isRefused() throws Exception {
        UUID product = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 3, 100))));

        correction(po, "reassign-vendor", false, "{\"vendorPartnerId\":\"" + createVendor() + "\"}")
                .refusedWith("already received");
    }

    private String billDocVendor(UUID billId) {
        try {
            return billDoc(billId).get("vendorPartnerId").asText();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
