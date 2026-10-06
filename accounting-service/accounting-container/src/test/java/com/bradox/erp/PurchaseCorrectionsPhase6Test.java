package com.bradox.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phase 6 for purchases: corrections to documents in a closed month are dated today. */
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseCorrectionsPhase6Test extends PurchaseScenarioSupport {

    private UUID postedBillDated(JsonNode po, String date) throws Exception {
        UUID bill = call(post("/api/v1/purchase/vendor-bills/from-po").contentType(MediaType.APPLICATION_JSON)
                .content("{\"purchaseOrderId\":\"" + po.get("id").asText() + "\",\"billDate\":\"" + date
                        + "\",\"dueDate\":\"" + date + "\"}")).andExpect(status().isOk()).id();
        call(post("/api/v1/purchase/vendor-bills/" + bill + "/post")).andExpect(status().isOk());
        return bill;
    }

    private void closeMonth(String start, String end) throws Exception {
        UUID period = call(post("/api/v1/companies/" + COMPANY_ID + "/fiscal-periods")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\":\"" + start + "\",\"endDate\":\"" + end + "\"}"))
                .andExpect(status().isOk()).id();
        call(post("/api/v1/companies/" + COMPANY_ID + "/fiscal-periods/" + period + "/close")).andExpect(status().isOk());
    }

    @Test
    void correctingABillOfAClosedMonth_isDatedTodayAndNewBillsInThatMonthAreRefused() throws Exception {
        UUID product = product();
        JsonNode po = receiveAll(confirm(createOrder(line(null, product, 4, 100))));
        UUID bill = postedBillDated(po, "2097-07-10");
        closeMonth("2097-07-01", "2097-07-31");

        // The bill is in a closed month, so the credit note is posted today, in the open month.
        call(post(orderUrl(po) + "/returns").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refund\":true,\"lines\":[{\"purchaseOrderLineId\":\"" + lineId(po, 0) + "\",\"qty\":1}]}"))
                .andExpect(status().isOk());

        JsonNode cn = creditNotes(bill).get(0);
        assertThat(cn.get("state").asText()).isEqualTo("POSTED");
        assertThat(cn.get("billDate").asText()).isEqualTo(LocalDate.now().toString());
        assertThat(billDoc(bill).get("amountResidual").decimalValue()).isEqualByComparingTo("300");

        // A new bill dated in the closed month is refused.
        JsonNode other = receiveAll(confirm(createOrder(line(null, product, 1, 100))));
        call(post("/api/v1/purchase/vendor-bills/from-po").contentType(MediaType.APPLICATION_JSON)
                .content("{\"purchaseOrderId\":\"" + other.get("id").asText()
                        + "\",\"billDate\":\"2097-07-20\",\"dueDate\":\"2097-08-20\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertConsistent(po);
        assertConsistent(other);
    }

    @Test
    void changeTermsOnABillOfAClosedMonth_postsTheCorrectionsToday() throws Exception {
        JsonNode po = receiveAll(confirm(createOrder(line(null, product(), 2, 100))));
        UUID bill = postedBillDated(po, "2097-05-10");
        closeMonth("2097-05-01", "2097-05-31");

        JsonNode result = call(post(orderUrl(po) + "/corrections/change-terms").param("preview", "false")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[{\"purchaseOrderLineId\":\"" + lineId(po, 0) + "\",\"unitPrice\":120}]}"))
                .andExpect(status().isOk()).json();

        assertThat(result.get("documents")).hasSize(2);
        for (JsonNode d : result.get("documents")) {
            assertThat(d.get("billDate").asText()).isEqualTo(LocalDate.now().toString());
        }
        assertThat(creditNotes(bill).get(0).get("state").asText()).isEqualTo("POSTED");
        assertThat(netBilled(reload(po))).isEqualByComparingTo("240");
        assertConsistent(po);
    }
}
