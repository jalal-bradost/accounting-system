package com.bradox.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Repair foundation end to end against the real schema: seeding, labor guide, lines, packages, inspection. */
@SpringBootTest
@AutoConfigureMockMvc
class RepairApiIntegrationTest extends PurchaseScenarioSupport {

    private static final String BASE = "/api/v1/repair";

    private JsonNode body(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req, String json) throws Exception {
        return call(req.contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().is2xxSuccessful()).json();
    }

    @Test
    void firstUseSeedsCategoriesAndInspectionTemplate() throws Exception {
        JsonNode settings = call(get(BASE + "/settings")).andExpect(status().isOk()).json();
        assertThat(settings.get("defaultInspectionTemplateId").isNull()).isFalse();
        assertThat(call(get(BASE + "/labor-categories")).json().size()).isGreaterThanOrEqualTo(5);
        JsonNode templates = call(get(BASE + "/inspection-templates")).json();
        assertThat(templates.get(0).get("items").size()).isGreaterThan(30);
    }

    @Test
    void guideImportThenOperationLineIsPricedAndLockedLinesBehave() throws Exception {
        String code = "T-" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode cat = body(post(BASE + "/labor-categories"), "{\"name\":\"Cat " + code + "\",\"hourlyRate\":30,\"active\":true}");
        JsonNode imported = body(post(BASE + "/labor-guide/import"), "{\"allOrNothing\":true,\"csv\":\"code,description_en,description_ar,description_ku,category,standard_minutes\\n"
                + code + ",Replace pads,,,Cat " + code + ",120\\n\"}");
        assertThat(imported.get("created").asInt()).isEqualTo(1);
        assertThat(imported.get("applied").asBoolean()).isTrue();

        JsonNode guide = call(get(BASE + "/labor-guide").param("q", code)).json();
        assertThat(guide.size()).isEqualTo(1);

        UUID order = UUID.randomUUID();
        JsonNode line = body(post(BASE + "/orders/" + order + "/lines"),
                "{\"type\":\"OPERATION\",\"laborGuideId\":\"" + guide.get(0).get("id").asText() + "\"}");
        assertThat(line.get("standardMinutes").asInt()).isEqualTo(120);
        assertThat(line.get("description").asText()).isEqualTo("Replace pads");

        JsonNode lines = call(get(BASE + "/orders/" + order + "/lines")).json();
        assertThat(lines.get("lines").size()).isEqualTo(1);

        call(delete(BASE + "/lines/" + line.get("id").asText())).andExpect(status().isNoContent());
        assertThat(call(get(BASE + "/orders/" + order + "/lines")).json().get("lines").size()).isZero();
    }

    @Test
    void packageAddsLinesAndArchivedPackageIsRefused() throws Exception {
        JsonNode pkg = body(post(BASE + "/packages"), "{\"name\":\"Oil service\",\"active\":true,\"lines\":["
                + "{\"type\":\"PART\",\"description\":\"Oil 5W30\",\"qty\":4},"
                + "{\"type\":\"OPERATION\",\"description\":\"Change oil\",\"standardMinutes\":30}]}");
        UUID order = UUID.randomUUID();
        JsonNode added = body(post(BASE + "/orders/" + order + "/lines/from-package"), "{\"packageId\":\"" + pkg.get("id").asText() + "\"}");
        assertThat(added.size()).isEqualTo(2);
        assertThat(added.get(0).get("sectionLabel").asText()).isEqualTo("Oil service");

        call(post(BASE + "/packages/" + pkg.get("id").asText() + "/archive")).andExpect(status().isOk());
        call(post(BASE + "/orders/" + order + "/lines/from-package").contentType(MediaType.APPLICATION_JSON)
                .content("{\"packageId\":\"" + pkg.get("id").asText() + "\"}")).andExpect(status().is4xxClientError());
    }

    @Test
    void inspectionIsSavedSignedOffAndThenLocked() throws Exception {
        UUID order = UUID.randomUUID();
        JsonNode started = call(post(BASE + "/orders/" + order + "/inspection")).andExpect(status().isOk()).json();
        JsonNode first = started.get("inspection").get("results").get(0);
        assertThat(started.get("summary").get("notChecked").asInt()).isGreaterThan(30);

        body(put(BASE + "/orders/" + order + "/inspection"), "{\"odometerKm\":120000,\"results\":[{\"section\":\""
                + first.get("section").asText() + "\",\"itemLabel\":\"" + first.get("itemLabel").asText() + "\",\"result\":\"URGENT\",\"note\":\"worn\"}]}");
        JsonNode finding = body(post(BASE + "/orders/" + order + "/findings"),
                "{\"severity\":\"URGENT\",\"description\":\"Pads worn\",\"recommendedAction\":\"Replace pads\",\"customerVisible\":true}");
        JsonNode quoted = body(post(BASE + "/orders/" + order + "/findings/" + finding.get("id").asText() + "/add-to-quote"),
                "{\"type\":\"OPERATION\"}");
        assertThat(quoted.get(0).get("fromFindingId").asText()).isEqualTo(finding.get("id").asText());

        call(post(BASE + "/orders/" + order + "/inspection/sign-off")).andExpect(status().isOk());
        boolean listed = false;
        for (JsonNode o : call(get(BASE + "/orders")).andExpect(status().isOk()).json()) {
            if (o.get("orderId").asText().equals(order.toString())) {
                listed = o.get("signedOff").asBoolean() && o.get("findings").asInt() == 1 && o.get("lines").asInt() == 1;
            }
        }
        assertThat(listed).isTrue();
        call(put(BASE + "/orders/" + order + "/inspection").contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"late\"}"))
                .andExpect(status().is4xxClientError());
    }
}
