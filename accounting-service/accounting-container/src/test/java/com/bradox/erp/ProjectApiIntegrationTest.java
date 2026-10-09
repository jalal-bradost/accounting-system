package com.bradox.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Projects end to end: default stages, tasks on the board, moving cards and stages, deleting. */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectApiIntegrationTest extends SalesScenarioSupport {

    private static final String BASE = "/api/v1/project";

    private JsonNode create(String url, String body) throws Exception {
        return call(post(url)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).json();
    }

    @Test
    void projectBoardWorksEndToEnd() throws Exception {
        JsonNode project = create(BASE + "/projects", "{\"name\":\"Office Design\",\"customerPartnerId\":\"" + customer
                + "\",\"startDate\":\"2026-10-01\",\"endDate\":\"2026-12-31\",\"color\":3}");
        String pid = project.get("id").asText();
        assertThat(project.get("taskCount").asLong()).isZero();

        JsonNode stages = call(get(BASE + "/projects/" + pid + "/stages")).andExpect(status().isOk()).json();
        assertThat(stages.size()).isEqualTo(3);
        assertThat(stages.get(0).get("name").asText()).isEqualTo("New");
        String newStage = stages.get(0).get("id").asText();
        String doing = stages.get(1).get("id").asText();

        JsonNode a = create(BASE + "/projects/" + pid + "/tasks", "{\"name\":\"Floor plan\",\"priority\":2}");
        JsonNode b = create(BASE + "/projects/" + pid + "/tasks", "{\"name\":\"Lighting\",\"priority\":0}");
        assertThat(a.get("stageId").asText()).isEqualTo(newStage);
        assertThat(a.get("customerPartnerId").asText()).isEqualTo(customer.toString());   // inherited from the project
        assertThat(b.get("sequence").asInt()).isEqualTo(1);

        // Drag "Lighting" to the top of In Progress, then "Floor plan" above it.
        call(post(BASE + "/tasks/" + b.get("id").asText() + "/move").contentType(MediaType.APPLICATION_JSON)
                .content("{\"stageId\":\"" + doing + "\",\"index\":0}")).andExpect(status().isOk());
        call(post(BASE + "/tasks/" + a.get("id").asText() + "/move").contentType(MediaType.APPLICATION_JSON)
                .content("{\"stageId\":\"" + doing + "\",\"index\":0}")).andExpect(status().isOk());
        JsonNode tasks = call(get(BASE + "/projects/" + pid + "/tasks")).andExpect(status().isOk()).json();
        assertThat(tasks.get(0).get("name").asText()).isEqualTo("Floor plan");
        assertThat(tasks.get(0).get("stageName").asText()).isEqualTo("In Progress");
        assertThat(tasks.get(1).get("name").asText()).isEqualTo("Lighting");

        // The chatter of the task records the stage change.
        JsonNode feed = call(get("/api/v1/activities").param("companyId", COMPANY_ID.toString())
                .param("model", "project.task").param("recordId", a.get("id").asText())).andExpect(status().isOk()).json();
        boolean logged = false;
        for (JsonNode m : feed.get("content")) {
            logged |= m.path("body").asText().contains("In Progress");
        }
        assertThat(logged).isTrue();

        // Edit a task.
        JsonNode edited = call(put(BASE + "/tasks/" + a.get("id").asText()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Floor plan v2\",\"stageId\":\"" + doing + "\",\"description\":\"Two options\","
                        + "\"deadline\":\"2026-11-01\",\"priority\":3}")).andExpect(status().isOk()).json();
        assertThat(edited.get("priority").asInt()).isEqualTo(3);
        assertThat(edited.get("deadline").asText()).isEqualTo("2026-11-01");

        // Stages: add, rename, move first; a stage with tasks cannot be deleted.
        JsonNode review = create(BASE + "/projects/" + pid + "/stages", "{\"name\":\"Review\"}");
        call(put(BASE + "/stages/" + review.get("id").asText()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Client review\"}")).andExpect(status().isOk());
        JsonNode moved = call(post(BASE + "/stages/" + review.get("id").asText() + "/move").param("index", "0"))
                .andExpect(status().isOk()).json();
        assertThat(moved.get(0).get("name").asText()).isEqualTo("Client review");
        call(delete(BASE + "/stages/" + doing)).andExpect(status().is4xxClientError());
        call(delete(BASE + "/stages/" + review.get("id").asText())).andExpect(status().isNoContent());

        assertThat(call(get(BASE + "/projects/" + pid)).json().get("taskCount").asLong()).isEqualTo(2);
        call(delete(BASE + "/tasks/" + b.get("id").asText())).andExpect(status().isNoContent());
        call(delete(BASE + "/projects/" + pid)).andExpect(status().isNoContent());
        call(get(BASE + "/projects/" + pid)).andExpect(status().isNotFound());
        call(get(BASE + "/tasks/" + a.get("id").asText())).andExpect(status().isNotFound());
    }

    @Test
    void refusesBadInput() throws Exception {
        call(post(BASE + "/projects")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\",\"startDate\":\"2026-10-02\",\"endDate\":\"2026-10-01\"}"))
                .andExpect(status().is4xxClientError());
        JsonNode project = create(BASE + "/projects", "{\"name\":\"R&D\"}");
        call(post(BASE + "/projects/" + project.get("id").asText() + "/tasks")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"T\",\"priority\":5}"))
                .andExpect(status().is4xxClientError());
    }
}
