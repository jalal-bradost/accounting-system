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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1 for purchases (the same guards sales got): orders, receipts and bills can no longer be
 * edited into states where they disagree. Every test ends by asserting the consistency checker
 * finds nothing wrong with its order.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseCorrectionsPhase1Test extends PurchaseScenarioSupport {

    // ---------------------------------------------------------------- confirmed, nothing received

    @Test
    void increaseQuantity_updatesTheSingleOpenReceipt() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 10, 100)));

        po = amend(po, line(lineId(po, 0), product, 15, 100)).andExpect(status().isOk()).json();

        assertThat(openReceipts(po)).hasSize(1);
        assertThat(openDemand(po, lineId(po, 0))).isEqualByComparingTo("15");
        assertConsistent(po);
    }

    @Test
    void decreaseQuantity_reducesTheOpenReceipt() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 10, 100)));

        po = amend(po, line(lineId(po, 0), product, 4, 100)).andExpect(status().isOk()).json();

        assertThat(openReceipts(po)).hasSize(1);
        assertThat(openDemand(po, lineId(po, 0))).isEqualByComparingTo("4");
        assertConsistent(po);
    }

    @Test
    void removeLine_dropsItFromTheOpenReceipt() throws Exception {
        UUID a = product();
        UUID b = product();
        JsonNode po = confirm(createOrder(line(null, a, 5, 100), line(null, b, 6, 100)));
        String keep = lineId(po, 0);
        String removed = lineId(po, 1);

        po = amend(po, line(keep, a, 5, 100)).andExpect(status().isOk()).json();

        assertThat(openReceipts(po)).hasSize(1);
        assertThat(openDemand(po, keep)).isEqualByComparingTo("5");
        assertThat(openDemand(po, removed)).isEqualByComparingTo("0");
        assertConsistent(po);
    }

    /** Receipts are valued at the order price, so a price change before receipt must reach the open receipt. */
    @Test
    void priceChangeBeforeReceipt_updatesTheCostOfTheOpenReceipt() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 5, 100)));
        assertThat(openCost(po, lineId(po, 0))).isEqualByComparingTo("100");

        po = amend(po, line(lineId(po, 0), product, 5, 80)).andExpect(status().isOk()).json();

        assertThat(openReceipts(po)).hasSize(1);
        assertThat(openCost(po, lineId(po, 0))).isEqualByComparingTo("80");
        assertConsistent(po);
    }

    @Test
    void cancelConfirmedOrder_cancelsOpenReceipts() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 3, 100)));

        po = call(post(orderUrl(po) + "/cancel")).andExpect(status().isOk()).json();

        assertThat(po.get("state").asText()).isEqualTo("CANCELLED");
        assertThat(openReceipts(po)).isEmpty();
        assertConsistent(po);
    }

    // ---------------------------------------------------------------- received, not billed

    @Test
    void changeVendorAfterReceipt_isRefused() throws Exception {
        UUID product = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 2, 100))));

        call(put(orderUrl(po)).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(createVendor(), po.get("rowVersion").asLong(), line(lineId(po, 0), product, 2, 100))))
                .refusedWith("Cannot change the vendor");
        assertConsistent(po);
    }

    @Test
    void increaseAfterPartialReceipt_updatesTheBackorder() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 10, 100)));
        UUID receipt = openReceipts(po).get(0);
        validate(receipt, firstMoveId(receipt), "4", true);
        po = reload(po);

        po = amend(po, line(lineId(po, 0), product, 12, 100)).andExpect(status().isOk()).json();

        assertThat(po.get("lines").get(0).get("qtyReceived").decimalValue()).isEqualByComparingTo("4");
        assertThat(openReceipts(po)).hasSize(1);
        assertThat(openDemand(po, lineId(po, 0))).isEqualByComparingTo("8");
        assertConsistent(po);
    }

    @Test
    void decreaseBelowReceived_isRefusedAndChangesNothing() throws Exception {
        UUID product = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 6, 100))));
        long version = po.get("rowVersion").asLong();

        amend(po, line(lineId(po, 0), product, 2, 100)).refusedWith("below the received quantity");

        JsonNode after = reload(po);
        assertThat(after.get("rowVersion").asLong()).isEqualTo(version);
        assertThat(after.get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("6");
        assertConsistent(after);
    }

    /** Price edits after receipt would leave the received goods at the old cost, so they are locked. */
    @Test
    void priceChangeAfterReceipt_isRefused() throws Exception {
        UUID product = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 5, 100))));

        amend(po, line(lineId(po, 0), product, 5, 90)).refusedWith("locked once goods are received or billed");
        amend(po, lineWithDiscount(lineId(po, 0), product, 5, 100, 10))
                .refusedWith("locked once goods are received or billed");
        assertConsistent(po);
    }

    @Test
    void receiveMoreThanOrdered_isRefused() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 3, 100)));
        UUID receipt = openReceipts(po).get(0);

        call(post("/api/v1/inventory/pickings/" + receipt + "/validate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"picks\":[{\"moveId\":\"" + firstMoveId(receipt) + "\",\"pickedQuantity\":5}]}"))
                .refusedWith("exceeds demand");

        assertThat(reload(po).get("lines").get(0).get("qtyReceived").decimalValue()).isEqualByComparingTo("0");
        assertConsistent(po);
    }

    @Test
    void closeRemaining_setsOrderedToReceivedAndCancelsTheBackorder() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 10, 100)));
        UUID receipt = openReceipts(po).get(0);
        validate(receipt, firstMoveId(receipt), "6", true);

        po = call(post(orderUrl(po) + "/close-remaining").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"vendor cannot ship the rest\"}")).andExpect(status().isOk()).json();

        assertThat(po.get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("6");
        assertThat(openReceipts(po)).isEmpty();
        assertConsistent(po);
    }

    @Test
    void cancelWithReceivedGoods_isRefusedUntilReturned() throws Exception {
        UUID product = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 2, 100))));

        call(post(orderUrl(po) + "/cancel")).refusedWith("goods have been received");

        UUID receipt = UUID.fromString(po.get("receiptPickingIds").get(0).asText());
        UUID ret = returnPicking(receipt, true).andExpect(status().isOk()).id();
        validate(ret, null, null, false);
        po = call(post(orderUrl(po) + "/cancel")).andExpect(status().isOk()).json();
        assertThat(po.get("state").asText()).isEqualTo("CANCELLED");
        assertConsistent(po);
    }

    // ---------------------------------------------------------------- draft and posted bills

    @Test
    void draftBillBlocksLineEditsUntilCancelled() throws Exception {
        UUID product = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 4, 100))));
        UUID draft = createBillFromOrder(po);
        po = reload(po);

        amend(po, line(lineId(po, 0), product, 9, 100)).refusedWith("draft bill or credit note exists");

        call(post("/api/v1/purchase/vendor-bills/" + draft + "/cancel")).andExpect(status().isOk());
        po = amend(reload(po), line(lineId(po, 0), product, 9, 100)).andExpect(status().isOk()).json();
        assertThat(po.get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("9");
        assertConsistent(po);
    }

    @Test
    void cancelOrderWithADraftBill_cancelsTheDraftToo() throws Exception {
        UUID service = serviceProduct();
        JsonNode po = confirm(createOrder(line(null, service, 3, 100)));
        UUID draft = createBillFromOrder(po);

        po = call(post(orderUrl(po) + "/cancel")).andExpect(status().isOk()).json();

        assertThat(po.get("state").asText()).isEqualTo("CANCELLED");
        assertThat(billDoc(draft).get("state").asText()).isEqualTo("CANCELLED");
        assertConsistent(po);
    }

    @Test
    void billedLine_priceAndDiscountAreLocked_quantityCannotDropBelowBilled() throws Exception {
        UUID product = product();
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product, 5, 100)))));

        amend(po, line(lineId(po, 0), product, 5, 90)).refusedWith("locked once goods are received or billed");
        amend(po, line(lineId(po, 0), product, 3, 100)).refusedWith("below the received quantity");

        // Increasing quantity stays allowed: the extra is received and billed later.
        po = amend(po, line(lineId(po, 0), product, 7, 100)).andExpect(status().isOk()).json();
        assertThat(openDemand(po, lineId(po, 0))).isEqualByComparingTo("2");
        assertConsistent(po);
    }

    @Test
    void serviceLineBilledWithoutReceipt_quantityCannotDropBelowBilled() throws Exception {
        UUID service = serviceProduct();
        JsonNode po = billAll(confirm(createOrder(line(null, service, 4, 100))));
        assertThat(po.get("lines").get(0).get("qtyInvoiced").decimalValue()).isEqualByComparingTo("4");

        amend(po, line(lineId(po, 0), service, 2, 100)).refusedWith("below the billed quantity");
        assertConsistent(po);
    }

    @Test
    void orderDiscountAfterBilling_isRefused() throws Exception {
        UUID product = product();
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product, 2, 100)))));
        String body = orderBody(vendor, po.get("rowVersion").asLong(), line(lineId(po, 0), product, 2, 100))
                .replaceFirst("\\{", "{\"orderDiscountType\":\"FIXED\",\"orderDiscountValue\":10,");

        call(put(orderUrl(po)).contentType(MediaType.APPLICATION_JSON).content(body))
                .refusedWith("order discount cannot change");
        assertConsistent(po);
    }

    // ---------------------------------------------------------------- concurrency

    @Test
    void editWithAStaleVersion_isRejected() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 2, 100)));
        long seen = po.get("rowVersion").asLong();

        call(put(orderUrl(po)).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(vendor, seen, line(lineId(po, 0), product, 3, 100))))
                .andExpect(status().isOk());
        call(put(orderUrl(po)).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(vendor, seen, line(lineId(po, 0), product, 4, 100))))
                .andExpect(status().isConflict());

        assertThat(reload(po).get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("3");
        assertConsistent(po);
    }
}
