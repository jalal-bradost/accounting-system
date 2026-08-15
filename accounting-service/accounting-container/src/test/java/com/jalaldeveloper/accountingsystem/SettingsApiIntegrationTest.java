package com.jalaldeveloper.accountingsystem;

import com.jalaldeveloper.accountingsystem.platform.bootstrap.PlatformDefaultAdminUserSeeder;
import com.jalaldeveloper.accountingsystem.platform.bootstrap.PlatformRbacSeeder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the Settings module: companies, roles, permissions, users,
 * and the {@code /api/v1/auth/me} + change-password flows. Security is disabled in
 * the test profile (default {@code app.security.enabled=false}); the resulting
 * permissive {@code AuthorizationPort} lets us drive the API without minting JWTs.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SettingsApiIntegrationTest {

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;
    private static final UUID ADMIN_USER_ID = PlatformDefaultAdminUserSeeder.DEFAULT_ADMIN_USER_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void permissionsCatalog_isGroupedByModule() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/platform/permissions")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        assertThat(body.get("total").asInt()).isPositive();
        assertThat(body.get("groups").isArray()).isTrue();

        boolean platformGroupSeen = false;
        for (JsonNode g : body.get("groups")) {
            String module = g.get("module").asText();
            assertThat(g.get("permissions").size()).isPositive();
            if ("platform".equals(module)) {
                platformGroupSeen = true;
                JsonNode perms = g.get("permissions");
                boolean hasCompanyRead = false;
                for (JsonNode p : perms) {
                    if ("platform.company.read".equals(p.get("code").asText())) {
                        hasCompanyRead = true;
                        break;
                    }
                }
                assertThat(hasCompanyRead).as("platform.company.read seeded").isTrue();
            }
        }
        assertThat(platformGroupSeen).isTrue();
    }

    @Test
    void companyMe_returnsSeededDemoCompany() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/platform/companies/me")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        assertThat(UUID.fromString(body.get("id").asText())).isEqualTo(COMPANY_ID);
        assertThat(body.get("name").asText()).isEqualTo("Demo Company");
        assertThat(body.get("defaultCurrency").asText()).isEqualTo("IQD");
    }

    @Test
    void updateCompanyProfile_persistsChanges() throws Exception {
        String body = "{\"name\":\"Demo Company\",\"legalName\":\"Demo Company LLC\","
                + "\"taxId\":\"TAX-001\",\"email\":\"hello@demo.local\",\"phone\":\"+1-555-0100\","
                + "\"addressLine1\":\"42 Pivot Way\",\"city\":\"Brooklyn\",\"state\":\"NY\","
                + "\"postalCode\":\"11201\",\"country\":\"US\",\"defaultCurrency\":\"USD\","
                + "\"locale\":\"en-US\",\"dateFormat\":\"yyyy-MM-dd\",\"numberFormat\":\"#,##0.00\","
                + "\"fiscalYearStartMonth\":1}";
        MvcResult res = mockMvc.perform(put("/api/v1/platform/companies/" + COMPANY_ID)
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode updated = json.readTree(res.getResponse().getContentAsString());
        assertThat(updated.get("taxId").asText()).isEqualTo("TAX-001");
        assertThat(updated.get("city").asText()).isEqualTo("Brooklyn");
        assertThat(updated.get("phone").asText()).isEqualTo("+1-555-0100");
    }

    @Test
    void setPeriodLockDate_storesAndClears() throws Exception {
        mockMvc.perform(patch("/api/v1/platform/companies/" + COMPANY_ID + "/period-lock")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodLockDate\":\"2025-12-31\"}"))
                .andExpect(status().isOk());

        MvcResult res = mockMvc.perform(get("/api/v1/platform/companies/me")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        assertThat(body.get("periodLockDate").asText()).isEqualTo("2025-12-31");

        mockMvc.perform(patch("/api/v1/platform/companies/" + COMPANY_ID + "/period-lock")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodLockDate\":null}"))
                .andExpect(status().isOk());
    }

    @Test
    void rolesAndUsers_endToEnd() throws Exception {
        // 1. Find platform.user.read permission to attach to a custom role.
        JsonNode catalog = json.readTree(mockMvc.perform(get("/api/v1/platform/permissions")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andReturn().getResponse().getContentAsString());
        UUID userReadId = findPermissionId(catalog, "platform.user.read");
        UUID userWriteId = findPermissionId(catalog, "platform.user.write");
        assertThat(userReadId).isNotNull();

        // 2. Create custom role.
        String code = "TESTROLE_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String createRoleBody = "{\"code\":\"" + code + "\",\"name\":\"Test Role\","
                + "\"description\":\"Custom role for ITs\",\"permissionIds\":[\""
                + userReadId + "\"]}";
        MvcResult roleRes = mockMvc.perform(post("/api/v1/platform/roles")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoleBody))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode role = json.readTree(roleRes.getResponse().getContentAsString());
        UUID roleId = UUID.fromString(role.get("id").asText());
        assertThat(role.get("systemRole").asBoolean()).isFalse();
        assertThat(role.get("permissionCodes").size()).isEqualTo(1);

        // 3. Update permissions on the role to include user.write.
        String permsBody = "{\"permissionIds\":[\"" + userReadId + "\",\"" + userWriteId + "\"]}";
        mockMvc.perform(put("/api/v1/platform/roles/" + roleId + "/permissions")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(permsBody))
                .andExpect(status().isOk());

        // 4. Create a user.
        String username = "tester_" + UUID.randomUUID().toString().substring(0, 6);
        String email = username + "@example.local";
        String createUserBody = "{\"username\":\"" + username + "\",\"email\":\"" + email + "\","
                + "\"displayName\":\"Tester " + username + "\",\"password\":\"initial-pass-1\"}";
        MvcResult userRes = mockMvc.perform(post("/api/v1/platform/users")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode user = json.readTree(userRes.getResponse().getContentAsString());
        UUID userId = UUID.fromString(user.get("id").asText());

        // 5. Assign the role.
        mockMvc.perform(put("/api/v1/platform/users/" + userId + "/roles")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[\"" + roleId + "\"]}"))
                .andExpect(status().isOk());

        // 6. /auth/me as that user should report two permissions.
        MvcResult meRes = mockMvc.perform(get("/api/v1/auth/me")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode me = json.readTree(meRes.getResponse().getContentAsString());
        assertThat(me.get("username").asText()).isEqualTo(username);
        assertThat(me.get("permissions").isArray()).isTrue();
        assertThat(containsValue(me.get("permissions"), "platform.user.read")).isTrue();
        assertThat(containsValue(me.get("permissions"), "platform.user.write")).isTrue();

        // 7. Update profile.
        mockMvc.perform(put("/api/v1/auth/profile")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Edited\"}"))
                .andExpect(status().isOk());

        // 8. Change own password (current then new).
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"initial-pass-1\",\"newPassword\":\"new-secret-1\"}"))
                .andExpect(status().isNoContent());

        // 9. Bad current password is rejected.
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong-pass\",\"newPassword\":\"new-secret-2\"}"))
                .andExpect(status().isBadRequest());

        // 10. Admin reset password (no current required).
        mockMvc.perform(post("/api/v1/platform/users/" + userId + "/reset-password")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"reset-pass-9\"}"))
                .andExpect(status().isNoContent());

        // 11. Deactivate then activate.
        mockMvc.perform(post("/api/v1/platform/users/" + userId + "/deactivate")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
        MvcResult statusRes = mockMvc.perform(get("/api/v1/platform/users/" + userId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json.readTree(statusRes.getResponse().getContentAsString()).get("active").asBoolean()).isFalse();
        mockMvc.perform(post("/api/v1/platform/users/" + userId + "/activate")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        // 12. List users includes our new one.
        MvcResult listRes = mockMvc.perform(get("/api/v1/platform/users")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .param("q", username))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = json.readTree(listRes.getResponse().getContentAsString());
        assertThat(list.get("totalElements").asInt()).isPositive();
        boolean found = false;
        for (JsonNode u : list.get("content")) {
            if (username.equals(u.get("username").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();

        // 13. Cleanup: clear roles + delete user, then delete role.
        mockMvc.perform(put("/api/v1/platform/users/" + userId + "/roles")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[]}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/platform/users/" + userId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/platform/roles/" + roleId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void createCompany_appearsInListForCreator() throws Exception {
        String name = "Acme " + UUID.randomUUID().toString().substring(0, 8);
        String body = "{\"name\":\"" + name + "\",\"legalName\":\"Acme LLC\","
                + "\"country\":\"US\",\"defaultCurrency\":\"USD\",\"locale\":\"en-US\","
                + "\"dateFormat\":\"yyyy-MM-dd\",\"numberFormat\":\"#,##0.00\",\"fiscalYearStartMonth\":1}";
        MvcResult createdRes = mockMvc.perform(post("/api/v1/platform/companies")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .header("X-User-Id", ADMIN_USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = json.readTree(createdRes.getResponse().getContentAsString());
        UUID newId = UUID.fromString(created.get("id").asText());
        assertThat(created.get("name").asText()).isEqualTo(name);

        MvcResult listRes = mockMvc.perform(get("/api/v1/platform/companies")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .header("X-User-Id", ADMIN_USER_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = json.readTree(listRes.getResponse().getContentAsString());
        boolean found = false;
        for (JsonNode c : list) {
            if (newId.toString().equals(c.get("id").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).as("new company visible to creator").isTrue();

        MvcResult rolesRes = mockMvc.perform(get("/api/v1/platform/roles")
                        .header("X-Company-Id", newId.toString())
                        .header("X-User-Id", ADMIN_USER_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode roles = json.readTree(rolesRes.getResponse().getContentAsString());
        boolean hasAdmin = false;
        for (JsonNode r : roles) {
            if ("ADMIN".equals(r.get("code").asText())) {
                hasAdmin = true;
                break;
            }
        }
        assertThat(hasAdmin).as("ADMIN role provisioned on new company").isTrue();
    }

    @Test
    void seededAdminUser_canAuthMe() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/auth/me")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .header("X-User-Id", ADMIN_USER_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode me = json.readTree(res.getResponse().getContentAsString());
        assertThat(me.get("username").asText()).isEqualTo("admin");
        assertThat(me.get("permissions").isArray()).isTrue();
        assertThat(me.get("permissions").size()).isPositive();
        assertThat(containsValue(me.get("permissions"), "platform.user.read")).isTrue();
    }

    @Test
    void deletingSystemRole_isRejected() throws Exception {
        MvcResult listRes = mockMvc.perform(get("/api/v1/platform/roles")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode roles = json.readTree(listRes.getResponse().getContentAsString());
        UUID adminRoleId = null;
        for (JsonNode r : roles) {
            if ("ADMIN".equals(r.get("code").asText())) {
                adminRoleId = UUID.fromString(r.get("id").asText());
                break;
            }
        }
        assertThat(adminRoleId).isNotNull();
        mockMvc.perform(delete("/api/v1/platform/roles/" + adminRoleId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isConflict());
    }

    private static UUID findPermissionId(JsonNode catalog, String code) {
        for (JsonNode g : catalog.get("groups")) {
            for (JsonNode p : g.get("permissions")) {
                if (code.equals(p.get("code").asText())) {
                    return UUID.fromString(p.get("id").asText());
                }
            }
        }
        return null;
    }

    private static boolean containsValue(JsonNode array, String value) {
        for (JsonNode n : array) {
            if (value.equals(n.asText())) {
                return true;
            }
        }
        return false;
    }
}
