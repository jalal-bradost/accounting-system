package com.bradox.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revaluation of open foreign-currency balances. In the test company USD is the base and IQD the
 * foreign currency: a 10,000 IQD bill booked at 0.0007 (7 USD) is carried at 7 until revalued.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CurrencyRevaluationTest extends PurchaseScenarioSupport {

    private static final String URL = "/api/v1/companies/" + COMPANY_ID + "/currency-revaluation";

    private UUID postedIqdBillVendor() throws Exception {
        String body = orderBody(vendor, null, line(null, product(), 1, 10000))
                .replace("\"currencyCode\":\"USD\"", "\"currencyCode\":\"IQD\"")
                .replaceFirst("\\{", "{\"exchangeRateToCompany\":0.0007,");
        JsonNode po = call(post("/api/v1/purchase/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).json();
        billAll(receiveAll(confirm(po)));
        return vendor;
    }

    /** Company-currency value of one IQD today (units of the base per IQD). */
    private BigDecimal iqdToday() throws Exception {
        String base = "/api/v1/companies/" + COMPANY_ID + "/currencies";
        JsonNode list = call(get(base)).andExpect(status().isOk()).json();
        UUID iqd = null;
        for (JsonNode c : list.has("content") ? list.get("content") : list) {
            if ("IQD".equals(c.get("code").asText())) iqd = UUID.fromString(c.get("id").asText());
        }
        JsonNode eff = call(get(base + "/" + iqd + "/rates/effective").param("date", LocalDate.now().toString()))
                .andExpect(status().isOk()).json();
        return BigDecimal.ONE.divide(eff.get("rate").decimalValue(), 12, RoundingMode.HALF_UP);
    }

    private JsonNode lineOf(JsonNode result, UUID partner) {
        for (JsonNode l : result.get("lines")) {
            if (partner.toString().equals(l.path("partnerId").asText())) return l;
        }
        return null;
    }

    @Test
    void previewShowsTheAdjustmentAndChangesNothing() throws Exception {
        UUID partner = postedIqdBillVendor();
        BigDecimal ap = accountBalance("430004");
        String today = LocalDate.now().toString();

        JsonNode result = call(post(URL).param("asOf", today).param("preview", "true"))
                .andExpect(status().isOk()).json();

        JsonNode line = lineOf(result, partner);
        assertThat(line).as("line for the vendor").isNotNull();
        assertThat(line.get("currencyCode").asText()).isEqualTo("IQD");
        assertThat(line.get("foreignBalance").decimalValue()).isEqualByComparingTo("-10000");
        assertThat(line.get("carriedValue").decimalValue()).isEqualByComparingTo("-7");
        BigDecimal revalued = new BigDecimal("-10000").multiply(iqdToday());
        assertThat(line.get("revaluedValue").decimalValue()).isCloseTo(revalued, org.assertj.core.data.Offset.offset(new BigDecimal("0.01")));
        assertThat(result.get("preview").asBoolean()).isTrue();
        assertThat(result.get("journalEntryId").isNull()).isTrue();
        assertThat(accountBalance("430004")).isEqualByComparingTo(ap);
    }

    @Test
    void posting_booksTheEntryAndItsReversal_andRunningAgainChangesNothing() throws Exception {
        UUID partner = postedIqdBillVendor();
        String today = LocalDate.now().toString();
        BigDecimal gain = accountBalance("430014");
        BigDecimal loss = accountBalance("430015");

        JsonNode first = call(post(URL).param("asOf", today).param("preview", "false"))
                .andExpect(status().isOk()).json();

        assertThat(first.get("journalEntryId").asText()).isNotBlank();
        assertThat(first.get("reversalJournalEntryId").asText()).isNotBlank();
        assertThat(lineOf(first, partner)).isNotNull();
        // The reversal the next day cancels the entry, so over the whole ledger nothing is left behind.
        assertThat(accountBalance("430014")).isEqualByComparingTo(gain);
        assertThat(accountBalance("430015")).isEqualByComparingTo(loss);

        JsonNode again = call(post(URL).param("asOf", today).param("preview", "false"))
                .andExpect(status().isOk()).json();
        assertThat(lineOf(again, partner)).as("already revalued for that date").isNull();
    }
}
