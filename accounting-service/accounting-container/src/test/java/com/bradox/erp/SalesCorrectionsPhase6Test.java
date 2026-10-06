package com.bradox.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 6: gift lines follow the same corrections as sold lines (stock back, gift expense reversed,
 * credit amount zero), and corrections to documents in a closed month are dated today.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SalesCorrectionsPhase6Test extends SalesScenarioSupport {

    // ---------------------------------------------------------------- 57: gift lines

    @Test
    void case57_returningAGift_putsStockBackAndReversesTheGiftExpense() throws Exception {
        UUID sold = stockedProduct(20);
        UUID gift = stockedProduct(20);
        BigDecimal expenseBefore = giftExpense();
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, sold, 2, 100), giftLine(gift, 3, 50)))));
        UUID invoice = postedInvoices(so).get(0);
        // 3 gifts cost 3 x the product's price on the books while given away.
        assertThat(giftExpense().subtract(expenseBefore)).isEqualByComparingTo("150");
        BigDecimal onHand = onHand(gift);

        so = returnGoods(so, true, false, lineId(so, 1), "2").andExpect(status().isOk()).json();

        assertThat(onHand(gift)).isEqualByComparingTo(onHand.add(new BigDecimal("2")));
        assertThat(giftExpense().subtract(expenseBefore)).isEqualByComparingTo("50");
        JsonNode cn = creditNotes(invoice).get(0);
        assertThat(cn.get("state").asText()).isEqualTo("POSTED");
        assertThat(cn.get("amountTotal").decimalValue()).isEqualByComparingTo("0");
        assertThat(cn.get("lines").get(0).get("isGift").asBoolean()).isTrue();
        // The paid line is untouched.
        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("200");
        assertConsistent(so);
    }

    @Test
    void case57_cancelWithDocuments_reversesGiftsAndSales() throws Exception {
        UUID sold = stockedProduct(20);
        UUID gift = stockedProduct(20);
        BigDecimal expenseBefore = giftExpense();
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, sold, 2, 100), giftLine(gift, 3, 50)))));

        JsonNode result = call(post(orderUrl(so) + "/corrections/cancel").param("preview", "false")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"test\"}"))
                .andExpect(status().isOk()).json();

        assertThat(result.get("order").get("state").asText()).isEqualTo("CANCELLED");
        assertThat(giftExpense()).isEqualByComparingTo(expenseBefore);
        assertConsistent(so);
    }

    @Test
    void case57_changingTheTermsOfAnInvoicedGiftLine_isRefused() throws Exception {
        UUID gift = stockedProduct(20);
        UUID sold = stockedProduct(20);
        JsonNode so = invoiceAll(deliverAll(confirm(createOrder(line(null, sold, 1, 100), giftLine(gift, 2, 50)))));

        call(post(orderUrl(so) + "/corrections/change-terms").param("preview", "false")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[{\"salesOrderLineId\":\"" + lineId(so, 1) + "\",\"unitPrice\":60}]}"))
                .refusedWith("locked once a line is invoiced");
        assertConsistent(so);
    }

    // ---------------------------------------------------------------- 58: closed period

    @Test
    void case58_correctingAnInvoiceOfAClosedMonth_isDatedTodayAndNewPostingsInThatMonthAreRefused() throws Exception {
        UUID product = stockedProduct(20);
        JsonNode so = deliverAll(confirm(createOrder(line(null, product, 4, 100))));
        UUID invoice = postedInvoiceDated(so, "2097-03-10");
        closeMonth("2097-03-01", "2097-03-31");

        // The invoice is in a closed month, so the correction is posted today, in the open month.
        returnGoods(so, true, false, lineId(so, 0), "1").andExpect(status().isOk());

        JsonNode cn = creditNotes(invoice).get(0);
        assertThat(cn.get("state").asText()).isEqualTo("POSTED");
        assertThat(cn.get("invoiceDate").asText()).isEqualTo(LocalDate.now().toString());
        assertThat(invoiceDoc(invoice).get("amountResidual").decimalValue()).isEqualByComparingTo("300");

        // A new document dated in the closed month is refused.
        JsonNode other = deliverAll(confirm(createOrder(line(null, product, 1, 100))));
        call(post("/api/v1/sales/customer-invoices/from-order").contentType(MediaType.APPLICATION_JSON)
                .content("{\"salesOrderId\":\"" + other.get("id").asText()
                        + "\",\"invoiceDate\":\"2097-03-20\",\"dueDate\":\"2097-04-20\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertConsistent(so);
        assertConsistent(other);
    }

    // ---------------------------------------------------------------- helpers

    private Line giftLine(UUID product, int qty, int price) {
        return new Line(null, product, qty, price, 0, true);
    }

    private UUID postedInvoiceDated(JsonNode so, String date) throws Exception {
        UUID invoice = call(post("/api/v1/sales/customer-invoices/from-order").contentType(MediaType.APPLICATION_JSON)
                .content("{\"salesOrderId\":\"" + so.get("id").asText() + "\",\"invoiceDate\":\"" + date
                        + "\",\"dueDate\":\"" + date + "\"}")).andExpect(status().isOk()).id();
        call(post("/api/v1/accounting/customer-invoices/" + invoice + "/post")).andExpect(status().isOk());
        return invoice;
    }

    private void closeMonth(String start, String end) throws Exception {
        UUID period = call(post("/api/v1/companies/" + COMPANY_ID + "/fiscal-periods")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\":\"" + start + "\",\"endDate\":\"" + end + "\"}"))
                .andExpect(status().isOk()).id();
        call(post("/api/v1/companies/" + COMPANY_ID + "/fiscal-periods/" + period + "/close")).andExpect(status().isOk());
    }

    /** Net debit on the Gift Expense account across the whole ledger. */
    private BigDecimal giftExpense() throws Exception {
        UUID account = null;
        for (JsonNode a : call(get("/api/v1/accounts").param("companyId", COMPANY_ID.toString())).json()) {
            if ("430008".equals(a.get("code").asText())) {
                account = UUID.fromString(a.get("id").asText());
            }
        }
        assertThat(account).as("gift expense account").isNotNull();
        BigDecimal net = BigDecimal.ZERO;
        for (JsonNode l : call(get("/api/v1/companies/" + COMPANY_ID + "/general-ledger")
                .param("from", "2000-01-01").param("to", "2100-12-31").param("accountId", account.toString()))
                .andExpect(status().isOk()).json().get("lines")) {
            net = net.add(l.get("debit").decimalValue()).subtract(l.get("credit").decimalValue());
        }
        return net;
    }
}
