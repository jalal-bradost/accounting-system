package com.jalaldeveloper.accountingsystem;

import com.jalaldeveloper.accountingsystem.web.DashboardController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CompanyCurrencyApiIntegrationTest {

    private static final UUID COMPANY_ID = DashboardController.DEFAULT_COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void list_seededUsdBaseAndIqd() throws Exception {
        MvcResult res =
                mockMvc.perform(
                                get("/api/v1/companies/" + COMPANY_ID + "/currencies")
                                        .header("X-Company-Id", COMPANY_ID.toString())
                                        .param("page", "0")
                                        .param("size", "20"))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        assertThat(body.get("content").isArray()).isTrue();
        assertThat(body.get("totalElements").asLong()).isGreaterThanOrEqualTo(2);

        boolean usd = false;
        boolean iqd = false;
        for (JsonNode row : body.get("content")) {
            String code = row.get("code").asText();
            if ("USD".equals(code)) {
                usd = true;
                assertThat(row.get("baseCurrency").asBoolean()).isTrue();
                assertThat(new BigDecimal(row.get("ratePerBase").asText())).isEqualByComparingTo(BigDecimal.ONE);
            }
            if ("IQD".equals(code)) {
                iqd = true;
                assertThat(row.get("baseCurrency").asBoolean()).isFalse();
            }
        }
        assertThat(usd).isTrue();
        assertThat(iqd).isTrue();
    }

    @Test
    void createEur_thenListed() throws Exception {
        String payload =
                "{\"code\":\"EUR\",\"symbol\":\"€\",\"name\":\"Euro\",\"ratePerBase\":\"0.92\",\"active\":true}";
        mockMvc.perform(
                        post("/api/v1/companies/" + COMPANY_ID + "/currencies")
                                .header("X-Company-Id", COMPANY_ID.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isOk());

        MvcResult res =
                mockMvc.perform(
                                get("/api/v1/companies/" + COMPANY_ID + "/currencies")
                                        .header("X-Company-Id", COMPANY_ID.toString())
                                        .param("q", "EUR"))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        assertThat(body.get("content").size()).isPositive();
        assertThat(body.get("content").get(0).get("code").asText()).isEqualTo("EUR");
    }

    @Test
    void seededIqd_anchorRateLineResolvesForLaterDates() throws Exception {
        UUID iqdId = currencyIdByCode("IQD");

        JsonNode history = listRates(iqdId);
        // Anchor is always present; other tests may have appended dated lines
        // — assert the anchor exists rather than a fixed history size.
        boolean anchor = false;
        for (JsonNode row : history) {
            if ("2010-01-01".equals(row.get("effectiveDate").asText())) {
                anchor = true;
                break;
            }
        }
        assertThat(anchor).as("Seeded anchor 2010-01-01 must always remain").isTrue();

        mockMvc.perform(
                        get("/api/v1/companies/" + COMPANY_ID + "/currencies/" + iqdId + "/rates/effective")
                                .header("X-Company-Id", COMPANY_ID.toString())
                                .param("date", "2009-12-31"))
                .andExpect(status().isNotFound());

        MvcResult eff =
                mockMvc.perform(
                                get("/api/v1/companies/" + COMPANY_ID + "/currencies/" + iqdId + "/rates/effective")
                                        .header("X-Company-Id", COMPANY_ID.toString())
                                        .param("date", "2010-06-30"))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(json.readTree(eff.getResponse().getContentAsString()).get("effectiveDate").asText())
                .isEqualTo("2010-01-01");
    }

    @Test
    void addingNewDatedRate_preservesPriorLines_andSwitchesEffectiveByDate() throws Exception {
        // Use a freshly created currency so test order doesn't matter.
        // The anchor line is dated "today" with rate 1.55; we add a future-dated line.
        UUID currencyId = createCurrency("AUD", "A$", "Australian dollar", "1.55");
        int historyBefore = listRates(currencyId).size();

        addRate(currencyId, "2030-01-01", "1.60");

        JsonNode history = listRates(currencyId);
        assertThat(history.size()).isEqualTo(historyBefore + 1);
        // Newest first
        assertThat(history.get(0).get("effectiveDate").asText()).isEqualTo("2030-01-01");
        assertThat(new BigDecimal(history.get(0).get("rate").asText()))
                .isEqualByComparingTo(new BigDecimal("1.60"));
        // Today's seeded anchor still present
        boolean anchorStillThere = false;
        for (JsonNode row : history) {
            BigDecimal rate = new BigDecimal(row.get("rate").asText());
            if (rate.compareTo(new BigDecimal("1.55")) == 0) {
                anchorStillThere = true;
                break;
            }
        }
        assertThat(anchorStillThere)
                .as("Adding a new dated line must not delete prior history")
                .isTrue();

        // A pre-anchor date must not resolve to anything.
        mockMvc.perform(
                        get("/api/v1/companies/" + COMPANY_ID + "/currencies/" + currencyId + "/rates/effective")
                                .header("X-Company-Id", COMPANY_ID.toString())
                                .param("date", "1999-01-01"))
                .andExpect(status().isNotFound());

        // A date well after the new line must hit it.
        MvcResult newer =
                mockMvc.perform(
                                get("/api/v1/companies/" + COMPANY_ID + "/currencies/" + currencyId + "/rates/effective")
                                        .header("X-Company-Id", COMPANY_ID.toString())
                                        .param("date", "2031-06-15"))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(json.readTree(newer.getResponse().getContentAsString()).get("effectiveDate").asText())
                .isEqualTo("2030-01-01");
    }

    @Test
    void addingRateForExistingDate_overwritesSameDateLineButKeepsOthers() throws Exception {
        UUID currencyId = createCurrency("CAD", "C$", "Canadian dollar", "1.36");
        addRate(currencyId, "2025-03-01", "1.40");
        addRate(currencyId, "2025-03-01", "1.42");
        JsonNode history = listRates(currencyId);
        long sameDay =
                java.util.stream.StreamSupport.stream(history.spliterator(), false)
                        .filter(n -> "2025-03-01".equals(n.get("effectiveDate").asText()))
                        .count();
        assertThat(sameDay).as("Same date should upsert into a single line").isEqualTo(1);

        // Resolve on/after that day → 1.42 (latest write wins for same date)
        MvcResult eff =
                mockMvc.perform(
                                get("/api/v1/companies/" + COMPANY_ID + "/currencies/" + currencyId + "/rates/effective")
                                        .header("X-Company-Id", COMPANY_ID.toString())
                                        .param("date", "2025-03-01"))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(new BigDecimal(json.readTree(eff.getResponse().getContentAsString()).get("rate").asText()))
                .isEqualByComparingTo(new BigDecimal("1.42"));
    }

    @Test
    void baseUsd_addingNonOneRate_isRejected() throws Exception {
        UUID usdId = currencyIdByCode("USD");
        mockMvc.perform(
                        post("/api/v1/companies/" + COMPANY_ID + "/currencies/" + usdId + "/rates")
                                .header("X-Company-Id", COMPANY_ID.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"effectiveDate\":\"2024-01-01\",\"rate\":\"1.5\"}"))
                .andExpect(status().is4xxClientError());
    }

    private UUID createCurrency(String code, String symbol, String name, String rate) throws Exception {
        String body =
                "{\"code\":\"" + code + "\",\"symbol\":\"" + symbol + "\",\"name\":\"" + name
                        + "\",\"ratePerBase\":\"" + rate + "\",\"active\":true}";
        MvcResult res =
                mockMvc.perform(
                                post("/api/v1/companies/" + COMPANY_ID + "/currencies")
                                        .header("X-Company-Id", COMPANY_ID.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isOk())
                        .andReturn();
        return UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }

    private void addRate(UUID currencyId, String date, String rate) throws Exception {
        mockMvc.perform(
                        post("/api/v1/companies/" + COMPANY_ID + "/currencies/" + currencyId + "/rates")
                                .header("X-Company-Id", COMPANY_ID.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"effectiveDate\":\"" + date + "\",\"rate\":\"" + rate + "\"}"))
                .andExpect(status().isOk());
    }

    private JsonNode listRates(UUID currencyId) throws Exception {
        MvcResult res =
                mockMvc.perform(
                                get("/api/v1/companies/" + COMPANY_ID + "/currencies/" + currencyId + "/rates")
                                        .header("X-Company-Id", COMPANY_ID.toString()))
                        .andExpect(status().isOk())
                        .andReturn();
        return json.readTree(res.getResponse().getContentAsString());
    }

    private UUID currencyIdByCode(String code) throws Exception {
        MvcResult res =
                mockMvc.perform(
                                get("/api/v1/companies/" + COMPANY_ID + "/currencies")
                                        .header("X-Company-Id", COMPANY_ID.toString())
                                        .param("q", code)
                                        .param("size", "10"))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        for (JsonNode row : body.get("content")) {
            if (code.equalsIgnoreCase(row.get("code").asText())) {
                return UUID.fromString(row.get("id").asText());
            }
        }
        throw new IllegalStateException("Currency " + code + " not seeded");
    }
}
