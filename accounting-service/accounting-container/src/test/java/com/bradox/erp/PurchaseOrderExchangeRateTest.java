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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A confirmed purchase order whose currency changes before any receipt or bill gets the new currency's rate. */
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseOrderExchangeRateTest extends PurchaseScenarioSupport {

    private BigDecimal expectedIqdRate() throws Exception {
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

    @Test
    void changingTheCurrencyOfAConfirmedOrderBeforeAnyReceipt_refreshesItsRate() throws Exception {
        UUID product = product();
        JsonNode po = confirm(createOrder(line(null, product, 2, 100)));
        assertThat(po.get("exchangeRateToCompany").decimalValue()).isEqualByComparingTo("1");

        String body = orderBody(vendor, po.get("rowVersion").asLong(), line(lineId(po, 0), product, 2, 100))
                .replace("\"currencyCode\":\"USD\"", "\"currencyCode\":\"IQD\"");
        JsonNode amended = call(put(orderUrl(po)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).json();

        assertThat(amended.get("currencyCode").asText()).isEqualTo("IQD");
        assertThat(amended.get("exchangeRateToCompany").decimalValue())
                .isCloseTo(expectedIqdRate(), org.assertj.core.data.Offset.offset(new BigDecimal("0.0000001")));
    }

    @Test
    void savingADraftPurchaseOrder_returnsTheFreshVersion() throws Exception {
        UUID product = product();
        JsonNode po = createOrder(line(null, product, 1, 100));
        JsonNode saved = amend(po, line(null, product, 2, 100)).andExpect(status().isOk()).json();
        assertThat(saved.get("rowVersion").asLong()).isEqualTo(reload(saved).get("rowVersion").asLong());
        amend(saved, line(null, product, 3, 100)).andExpect(status().isOk());
    }
}
