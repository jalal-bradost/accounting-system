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
 * Phase 3 for purchases: price, discount and tax changes after receipt or billing are done as a
 * vendor credit note plus a new bill in one step, and the price difference on goods that were
 * already received lands on the Purchase Price Variance account, never on Stock Input (GR/IR).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseCorrectionsPhase3Test extends PurchaseScenarioSupport {

    private static final String VARIANCE = "430026";
    private static final String STOCK_INPUT = "430011";
    private static final String INVENTORY = "430010";

    private Result changeTerms(JsonNode po, boolean preview, String lineJson) throws Exception {
        return call(post(orderUrl(po) + "/corrections/change-terms").param("preview", String.valueOf(preview))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"vendor corrected the invoice\",\"lines\":[{\"purchaseOrderLineId\":\""
                        + lineId(po, 0) + "\"," + lineJson + "}]}"));
    }

    @Test
    void priceIncrease_afterReceiptAndBilling_creditsRebillsAndBooksTheDifferenceAsVariance() throws Exception {
        UUID product = product();
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product, 5, 100)))));
        BigDecimal variance = accountBalance(VARIANCE);
        BigDecimal stockInput = accountBalance(STOCK_INPUT);
        BigDecimal inventory = accountBalance(INVENTORY);
        UUID bill = postedBills(po).get(0);

        JsonNode result = changeTerms(po, false, "\"unitPrice\":120").andExpect(status().isOk()).json();

        assertThat(result.get("documents")).hasSize(2);
        assertThat(result.get("order").get("lines").get(0).get("unitPrice").decimalValue()).isEqualByComparingTo("120");
        JsonNode cn = creditNotes(bill).get(0);
        assertThat(cn.get("state").asText()).isEqualTo("POSTED");
        assertThat(cn.get("amountTotal").decimalValue()).isEqualByComparingTo("500");
        po = reload(po);
        assertThat(netBilled(po)).isEqualByComparingTo("600");
        // 5 units cost 100 on receipt and are now billed at 120: 100 of variance, Stock Input stays clear.
        assertThat(accountBalance(VARIANCE).subtract(variance)).isEqualByComparingTo("100");
        assertThat(accountBalance(STOCK_INPUT)).isEqualByComparingTo(stockInput);
        assertThat(accountBalance(INVENTORY)).isEqualByComparingTo(inventory);
        assertConsistent(po);
    }

    @Test
    void priceDecrease_booksANegativeVariance() throws Exception {
        UUID product = product();
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product, 4, 100)))));
        BigDecimal variance = accountBalance(VARIANCE);
        BigDecimal stockInput = accountBalance(STOCK_INPUT);

        changeTerms(po, false, "\"unitPrice\":90").andExpect(status().isOk());

        po = reload(po);
        assertThat(netBilled(po)).isEqualByComparingTo("360");
        assertThat(accountBalance(VARIANCE).subtract(variance)).isEqualByComparingTo("-40");
        assertThat(accountBalance(STOCK_INPUT)).isEqualByComparingTo(stockInput);
        assertConsistent(po);
    }

    @Test
    void discountChange_afterBilling_isRebilledAtTheNewDiscount() throws Exception {
        UUID product = product();
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product, 5, 100)))));
        BigDecimal variance = accountBalance(VARIANCE);

        changeTerms(po, false, "\"discountType\":\"PERCENT\",\"discountValue\":10").andExpect(status().isOk());

        po = reload(po);
        assertThat(netBilled(po)).isEqualByComparingTo("450");
        assertThat(accountBalance(VARIANCE).subtract(variance)).isEqualByComparingTo("-50");
        assertConsistent(po);
    }

    @Test
    void priceChange_afterReceiptButBeforeBilling_changesNothingYet_andTheBillBooksTheVariance() throws Exception {
        UUID product = product();
        BigDecimal stockInput = accountBalance(STOCK_INPUT);
        BigDecimal variance = accountBalance(VARIANCE);
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 5, 100))));

        JsonNode result = changeTerms(po, false, "\"unitPrice\":90").andExpect(status().isOk()).json();

        assertThat(result.get("documents")).isEmpty();
        po = billAll(reload(po));
        assertThat(netBilled(po)).isEqualByComparingTo("450");
        assertThat(accountBalance(VARIANCE).subtract(variance)).isEqualByComparingTo("-50");
        assertThat(accountBalance(STOCK_INPUT)).isEqualByComparingTo(stockInput);
        assertConsistent(po);
    }

    @Test
    void priceChange_beforeAnythingHappened_updatesTheOpenReceiptCost() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 5, 100)));

        JsonNode result = changeTerms(po, false, "\"unitPrice\":110").andExpect(status().isOk()).json();

        assertThat(result.get("documents")).isEmpty();
        po = reload(po);
        assertThat(openReceipts(po)).hasSize(1);
        assertThat(openCost(po, lineId(po, 0))).isEqualByComparingTo("110");
        assertConsistent(po);
    }

    @Test
    void preview_showsTheDocumentsAndChangesNothing() throws Exception {
        UUID product = product();
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product, 5, 100)))));
        BigDecimal variance = accountBalance(VARIANCE);
        UUID bill = postedBills(po).get(0);

        JsonNode preview = changeTerms(po, true, "\"unitPrice\":120").andExpect(status().isOk()).json();

        assertThat(preview.get("preview").asBoolean()).isTrue();
        assertThat(preview.get("documents")).hasSize(2);
        assertThat(preview.get("documents").get(1).get("amountTotal").decimalValue()).isEqualByComparingTo("600");
        po = reload(po);
        assertThat(po.get("lines").get(0).get("unitPrice").decimalValue()).isEqualByComparingTo("100");
        assertThat(creditNotes(bill)).isEmpty();
        assertThat(netBilled(po)).isEqualByComparingTo("500");
        assertThat(accountBalance(VARIANCE)).isEqualByComparingTo(variance);
        assertConsistent(po);
    }

    @Test
    void returnAfterAPriceChange_reversesTheVarianceInProportion() throws Exception {
        UUID product = product();
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product, 5, 100)))));
        BigDecimal variance = accountBalance(VARIANCE);
        BigDecimal stockInput = accountBalance(STOCK_INPUT);
        changeTerms(po, false, "\"unitPrice\":120").andExpect(status().isOk());
        po = reload(po);

        call(post(orderUrl(po) + "/returns").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refund\":true,\"lines\":[{\"purchaseOrderLineId\":\"" + lineId(po, 0) + "\",\"qty\":2}]}"))
                .andExpect(status().isOk());

        // 3 of the 5 units remain: 3 x (120 - 100) = 60 of variance, Stock Input still clear.
        assertThat(accountBalance(VARIANCE).subtract(variance)).isEqualByComparingTo("60");
        assertThat(accountBalance(STOCK_INPUT)).isEqualByComparingTo(stockInput);
        assertConsistent(reload(po));
    }

    @Test
    void changingToTheSameTerms_isRefusedAsNothingToChange() throws Exception {
        UUID product = product();
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product, 2, 100)))));

        changeTerms(po, false, "\"unitPrice\":100").refusedWith("nothing to change");

        assertThat(netBilled(reload(po))).isEqualByComparingTo("200");
    }

    @Test
    void aDraftBillInTheWay_blocksTheCorrection() throws Exception {
        UUID product = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 4, 100))));
        createBillFromOrder(po);

        changeTerms(po, false, "\"unitPrice\":120").refusedWith("draft bill");
    }

    @Test
    void serviceLine_priceChangeAfterBilling_needsNoVariance() throws Exception {
        UUID service = serviceProduct();
        JsonNode po = billAll(confirm(createOrder(line(null, service, 3, 100))));
        BigDecimal variance = accountBalance(VARIANCE);

        changeTerms(po, false, "\"unitPrice\":150").andExpect(status().isOk());

        po = reload(po);
        assertThat(netBilled(po)).isEqualByComparingTo("450");
        assertThat(accountBalance(VARIANCE)).isEqualByComparingTo(variance);
        assertConsistent(po);
    }
}
