package com.bradox.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Starred (favourite) records are remembered per user and model. */
@SpringBootTest
@AutoConfigureMockMvc
class UserStarTest extends PurchaseScenarioSupport {

    private static final UUID ME = UUID.randomUUID();
    private static final UUID SOMEONE_ELSE = UUID.randomUUID();

    private boolean isStarred(String model, UUID id) throws Exception {
        return isStarred(ME, model, id);
    }

    private boolean isStarred(UUID user, String model, UUID id) throws Exception {
        JsonNode r = call(get("/api/v1/platform/stars").param("model", model).header("X-User-Id", user.toString()))
                .andExpect(status().isOk()).json();
        for (JsonNode n : r.get("recordIds")) {
            if (id.toString().equals(n.asText())) return true;
        }
        return false;
    }

    private void star(String model, UUID id, boolean starred) throws Exception {
        star(ME, model, id, starred);
    }

    private void star(UUID user, String model, UUID id, boolean starred) throws Exception {
        call(put("/api/v1/platform/stars").header("X-User-Id", user.toString()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"model\":\"" + model + "\",\"recordId\":\"" + id + "\",\"starred\":" + starred + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void starringARecord_isRemembered_andUnstarringRemovesIt() throws Exception {
        UUID id = UUID.randomUUID();
        assertThat(isStarred("sales.order", id)).isFalse();

        star("sales.order", id, true);
        assertThat(isStarred("sales.order", id)).isTrue();

        star("sales.order", id, true); // twice is harmless
        assertThat(isStarred("sales.order", id)).isTrue();

        star("sales.order", id, false);
        assertThat(isStarred("sales.order", id)).isFalse();
        star("sales.order", id, false);
    }

    @Test
    void starsAreKeptPerModel() throws Exception {
        UUID id = UUID.randomUUID();
        star("inventory.product", id, true);
        assertThat(isStarred("inventory.product", id)).isTrue();
        assertThat(isStarred("sales.order", id)).isFalse();
        star("inventory.product", id, false);
    }

    @Test
    void starsArePersonal() throws Exception {
        UUID id = UUID.randomUUID();
        star(ME, "sales.order", id, true);
        assertThat(isStarred(ME, "sales.order", id)).isTrue();
        assertThat(isStarred(SOMEONE_ELSE, "sales.order", id)).isFalse();
        star(ME, "sales.order", id, false);
    }

    @Test
    void anInvalidModelNameIsRefused() throws Exception {
        call(put("/api/v1/platform/stars").header("X-User-Id", ME.toString()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"model\":\"Bad Model!\",\"recordId\":\"" + UUID.randomUUID() + "\",\"starred\":true}"))
                .andExpect(status().is4xxClientError());
    }
}
