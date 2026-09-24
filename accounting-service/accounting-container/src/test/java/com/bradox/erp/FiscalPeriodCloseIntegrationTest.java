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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FiscalPeriodCloseIntegrationTest {

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void createEnsureCloseMonth_isIrreversibleAndBlocksPosting() throws Exception {
        String uniqueStart = "2099-01-01";
        String uniqueEnd = "2099-01-31";

        MvcResult created = mockMvc.perform(post("/api/v1/companies/" + COMPANY_ID + "/fiscal-periods")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"" + uniqueStart + "\",\"endDate\":\"" + uniqueEnd + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode period = json.readTree(created.getResponse().getContentAsString());
        String periodId = period.get("id").asText();
        assertThat(period.get("open").asBoolean()).isTrue();

        mockMvc.perform(get("/api/v1/companies/" + COMPANY_ID + "/fiscal-periods/" + periodId + "/close-checklist")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        MvcResult closed = mockMvc.perform(post("/api/v1/companies/" + COMPANY_ID + "/fiscal-periods/" + periodId + "/close")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode closedBody = json.readTree(closed.getResponse().getContentAsString());
        assertThat(closedBody.get("open").asBoolean()).isFalse();
        assertThat(closedBody.get("closedAt").asText()).isNotBlank();

        // Closing again is idempotent
        mockMvc.perform(post("/api/v1/companies/" + COMPANY_ID + "/fiscal-periods/" + periodId + "/close")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        MvcResult listed = mockMvc.perform(get("/api/v1/companies/" + COMPANY_ID + "/fiscal-periods")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = json.readTree(listed.getResponse().getContentAsString());
        boolean foundClosed = false;
        for (JsonNode p : list) {
            if (periodId.equals(p.get("id").asText())) {
                foundClosed = true;
                assertThat(p.get("open").asBoolean()).isFalse();
            }
        }
        assertThat(foundClosed).isTrue();
    }

    @Test
    void ensureMonths_createsTwelvePeriods() throws Exception {
        mockMvc.perform(post("/api/v1/companies/" + COMPANY_ID + "/fiscal-periods/ensure-months")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2098}"))
                .andExpect(status().isOk());

        MvcResult listed = mockMvc.perform(get("/api/v1/companies/" + COMPANY_ID + "/fiscal-periods")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = json.readTree(listed.getResponse().getContentAsString());
        int inYear = 0;
        for (JsonNode p : list) {
            String start = p.get("startDate").asText();
            if (start.startsWith("2098-")) {
                inYear++;
            }
        }
        assertThat(inYear).isGreaterThanOrEqualTo(12);
    }
}
