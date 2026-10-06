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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Orders carry the exchange rate of their currency: it is looked up when the order is created,
 * follows a currency change on a draft, and is never left at 1 for a foreign currency.
 * (In the test company the base currency is USD and IQD is the foreign one.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderExchangeRateTest extends SalesScenarioSupport {

    /** What the system should use for IQD today: 1 / the rate per base on the currency's rate line. */
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

    private String withCurrency(String body, String code) {
        return body.replace("\"currencyCode\":\"USD\"", "\"currencyCode\":\"" + code + "\"");
    }

    @Test
    void salesOrderInAForeignCurrency_getsTheRateFromTheCurrencyTable() throws Exception {
        UUID product = stockedProduct(10);
        JsonNode so = call(post("/api/v1/sales/orders").contentType(MediaType.APPLICATION_JSON)
                .content(withCurrency(orderBody(customer, null, line(null, product, 2, 1000)), "IQD")))
                .andExpect(status().isOk()).json();

        BigDecimal rate = so.get("exchangeRateToCompany").decimalValue();
        assertThat(rate).isNotEqualByComparingTo("1");
        assertThat(rate).isCloseTo(expectedIqdRate(), org.assertj.core.data.Offset.offset(new BigDecimal("0.0000001")));
        assertThat(confirm(so).get("exchangeRateToCompany").decimalValue())
                .isCloseTo(expectedIqdRate(), org.assertj.core.data.Offset.offset(new BigDecimal("0.0000001")));
    }

    @Test
    void changingADraftSalesOrderCurrency_refreshesItsRate() throws Exception {
        UUID product = stockedProduct(10);
        JsonNode so = createOrder(line(null, product, 1, 100));
        assertThat(so.get("exchangeRateToCompany").decimalValue()).isEqualByComparingTo("1");

        JsonNode toIqd = call(put(orderUrl(so)).contentType(MediaType.APPLICATION_JSON)
                .content(withCurrency(orderBody(customer, so.get("rowVersion").asLong(), line(null, product, 1, 100)), "IQD")))
                .andExpect(status().isOk()).json();
        assertThat(toIqd.get("exchangeRateToCompany").decimalValue())
                .isCloseTo(expectedIqdRate(), org.assertj.core.data.Offset.offset(new BigDecimal("0.0000001")));
        toIqd = reload(toIqd);

        JsonNode back = call(put(orderUrl(toIqd)).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(customer, toIqd.get("rowVersion").asLong(), line(null, product, 1, 100))))
                .andExpect(status().isOk()).json();
        assertThat(back.get("exchangeRateToCompany").decimalValue()).isEqualByComparingTo("1");
    }

    @Test
    void anExplicitRateOnASalesOrder_isKept() throws Exception {
        UUID product = stockedProduct(10);
        String body = withCurrency(orderBody(customer, null, line(null, product, 1, 100)), "IQD")
                .replaceFirst("\\{", "{\"exchangeRateToCompany\":0.0007,");
        JsonNode so = call(post("/api/v1/sales/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).json();
        assertThat(so.get("exchangeRateToCompany").decimalValue()).isEqualByComparingTo("0.0007");
    }

    /** The response of a draft save carries the version after the save, so the next save is not refused. */
    @Test
    void savingADraftSalesOrder_returnsTheFreshVersion() throws Exception {
        UUID product = stockedProduct(10);
        JsonNode so = createOrder(line(null, product, 1, 100));
        JsonNode saved = amend(so, line(null, product, 2, 100)).andExpect(status().isOk()).json();
        assertThat(saved.get("rowVersion").asLong()).isEqualTo(reload(saved).get("rowVersion").asLong());
        amend(saved, line(null, product, 3, 100)).andExpect(status().isOk());
    }

    /** The order list carries the submit time, which the salesperson's day total is built on. */
    @Test
    void theOrderListShowsWhenAConfirmedOrderWasSubmitted() throws Exception {
        UUID product = stockedProduct(10);
        JsonNode so = confirm(createOrder(line(null, product, 1, 100)));
        JsonNode page = call(get("/api/v1/sales/orders").param("size", "20")).andExpect(status().isOk()).json();
        JsonNode row = null;
        for (JsonNode r : page.get("content")) {
            if (so.get("id").asText().equals(r.get("id").asText())) row = r;
        }
        assertThat(row).isNotNull();
        assertThat(row.get("confirmedAt").asText()).isNotBlank();
        assertThat(row.get("amountTotal").decimalValue()).isEqualByComparingTo("100");
    }

    /** The order list can be limited to an order date, and orders without that filter are all still listed. */
    @Test
    void theOrderListCanBeLimitedToAnOrderDate() throws Exception {
        UUID product = stockedProduct(10);
        JsonNode so = createOrder(line(null, product, 1, 100));
        String today = LocalDate.now().toString();
        String yesterday = LocalDate.now().minusDays(1).toString();

        JsonNode withToday = call(get("/api/v1/sales/orders").param("size", "500")
                .param("orderDateFrom", today).param("orderDateTo", today)).andExpect(status().isOk()).json();
        JsonNode withYesterday = call(get("/api/v1/sales/orders").param("size", "500")
                .param("orderDateFrom", yesterday).param("orderDateTo", yesterday)).andExpect(status().isOk()).json();
        JsonNode all = call(get("/api/v1/sales/orders").param("size", "500")).andExpect(status().isOk()).json();

        assertThat(contains(withToday, so)).isTrue();
        assertThat(contains(withYesterday, so)).isFalse();
        assertThat(contains(all, so)).isTrue();
    }

    private static boolean contains(JsonNode page, JsonNode order) {
        for (JsonNode r : page.get("content")) {
            if (order.get("id").asText().equals(r.get("id").asText())) return true;
        }
        return false;
    }
}
