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

/** Phase 2 for purchases: returns to the vendor are atomic (return, credit note and order update in one step). */
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseCorrectionsPhase2Test extends PurchaseScenarioSupport {

    private Result returnGoods(JsonNode po, boolean refund, String... lineQty) throws Exception {
        StringBuilder lines = new StringBuilder();
        for (int i = 0; i < lineQty.length; i += 2) {
            lines.append(i > 0 ? "," : "").append("{\"purchaseOrderLineId\":\"").append(lineQty[i])
                    .append("\",\"qty\":").append(lineQty[i + 1]).append("}");
        }
        return call(post(orderUrl(po) + "/returns").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refund\":" + refund + ",\"lines\":[" + lines + "]}"));
    }

    @Test
    void refundReturnOfBilledGoods_postsTheCreditNoteAtOnceAndReducesTheOrder() throws Exception {
        UUID product = product();
        JsonNode po = billAll(receiveAll(confirm(createOrder(line(null, product, 5, 100)))));
        UUID bill = postedBills(po).get(0);
        BigDecimal onHand = onHand(product);

        po = returnGoods(po, true, lineId(po, 0), "2").andExpect(status().isOk()).json();

        assertThat(onHand(product)).isEqualByComparingTo(onHand.subtract(new BigDecimal("2")));
        assertThat(po.get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("3");
        JsonNode cn = creditNotes(bill).get(0);
        assertThat(cn.get("state").asText()).isEqualTo("POSTED");
        assertThat(cn.get("amountTotal").decimalValue()).isEqualByComparingTo("200");
        assertThat(netBilled(po)).isEqualByComparingTo("300");
        assertConsistent(po);
    }

    @Test
    void refundReturnOfUnbilledGoods_createsNoCreditNote() throws Exception {
        UUID product = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 4, 100))));

        po = returnGoods(po, true, lineId(po, 0), "1").andExpect(status().isOk()).json();

        assertThat(po.get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("3");
        assertThat(netBilled(po)).isEqualByComparingTo("0");
        assertConsistent(po);
    }

    @Test
    void returnOfOneLine_leavesTheOtherLinesOfTheReceiptAlone() throws Exception {
        UUID a = product();
        UUID b = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, a, 3, 100), line(null, b, 4, 100))));
        BigDecimal bBefore = onHand(b);

        po = returnGoods(po, true, lineId(po, 0), "1").andExpect(status().isOk()).json();

        assertThat(onHand(b)).isEqualByComparingTo(bBefore);
        assertThat(po.get("lines").get(1).get("qtyReceived").decimalValue()).isEqualByComparingTo("4");
        assertConsistent(po);
    }

    @Test
    void replaceReturn_keepsTheOrderAndCreatesAReceiptForTheReplacement() throws Exception {
        UUID product = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 5, 100))));

        po = returnGoods(po, false, lineId(po, 0), "2").andExpect(status().isOk()).json();

        assertThat(po.get("lines").get(0).get("qtyOrdered").decimalValue()).isEqualByComparingTo("5");
        assertThat(openDemand(po, lineId(po, 0))).isEqualByComparingTo("2");
        assertConsistent(po);
    }

    @Test
    void returnMoreThanReceived_isRefusedAndChangesNothing() throws Exception {
        UUID product = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 3, 100))));
        BigDecimal onHand = onHand(product);

        returnGoods(po, true, lineId(po, 0), "4").refusedWith("more than was received");

        assertThat(onHand(product)).isEqualByComparingTo(onHand);
        assertConsistent(po);
    }

    @Test
    void secondReturnCannotExceedWhatIsLeft() throws Exception {
        UUID product = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 3, 100))));
        po = returnGoods(po, true, lineId(po, 0), "2").andExpect(status().isOk()).json();

        returnGoods(po, true, lineId(po, 0), "2").refusedWith("more than was received");
        assertConsistent(po);
    }
}
