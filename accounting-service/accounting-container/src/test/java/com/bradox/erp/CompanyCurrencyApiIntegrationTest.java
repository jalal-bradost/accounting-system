package com.bradox.erp;

import com.bradox.erp.platform.bootstrap.PlatformRbacSeeder;
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

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;

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
    void ratesCanOnlyBeCreatedForToday_pastAndFutureAreRefused() throws Exception {
        UUID currencyId = createCurrency("AUD", "A$", "Australian dollar", "1.55");
        int historyBefore = listRates(currencyId).size();

        for (String date : new String[] { java.time.LocalDate.now().minusDays(30).toString(),
                java.time.LocalDate.now().plusDays(30).toString(), "2030-01-01" }) {
            mockMvc.perform(
                            post("/api/v1/companies/" + COMPANY_ID + "/currencies/" + currencyId + "/rates")
                                    .header("X-Company-Id", COMPANY_ID.toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"effectiveDate\":\"" + date + "\",\"rate\":\"1.60\"}"))
                    .andExpect(status().is4xxClientError());
        }
        assertThat(listRates(currencyId)).as("history is unchanged by refused rates").hasSize(historyBefore);
    }

    @Test
    void addingTodaysRate_overwritesTodaysLineButKeepsHistory() throws Exception {
        UUID currencyId = createCurrency("CAD", "C$", "Canadian dollar", "1.36");
        String today = java.time.LocalDate.now().toString();
        addRate(currencyId, today, "1.40");
        addRate(currencyId, today, "1.42");
        JsonNode history = listRates(currencyId);
        long sameDay =
                java.util.stream.StreamSupport.stream(history.spliterator(), false)
                        .filter(n -> today.equals(n.get("effectiveDate").asText()))
                        .count();
        assertThat(sameDay).as("Same date should upsert into a single line").isEqualTo(1);

        MvcResult eff =
                mockMvc.perform(
                                get("/api/v1/companies/" + COMPANY_ID + "/currencies/" + currencyId + "/rates/effective")
                                        .header("X-Company-Id", COMPANY_ID.toString())
                                        .param("date", today))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(new BigDecimal(json.readTree(eff.getResponse().getContentAsString()).get("rate").asText()))
                .isEqualByComparingTo(new BigDecimal("1.42"));
        // History before today stays readable and unresolved dates stay unresolved.
        mockMvc.perform(
                        get("/api/v1/companies/" + COMPANY_ID + "/currencies/" + currencyId + "/rates/effective")
                                .header("X-Company-Id", COMPANY_ID.toString())
                                .param("date", "1999-01-01"))
                .andExpect(status().isNotFound());
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
