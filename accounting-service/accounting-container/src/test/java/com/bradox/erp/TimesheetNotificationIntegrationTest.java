package com.bradox.erp;

import com.bradox.erp.platform.bootstrap.PlatformDefaultAdminUserSeeder;
import com.bradox.erp.platform.bootstrap.PlatformRbacSeeder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TSH-05 notifications and the weekly reminder, checked in the real activities inbox: a submitted week reaches the
 * manager, a decision closes that to-do and informs the employee, and a reminder goes out once per week.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimesheetNotificationIntegrationTest {

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;
    private static final UUID ADMIN_USER = PlatformDefaultAdminUserSeeder.DEFAULT_ADMIN_USER_ID;
    private static final String TS = "/api/v1/timesheet";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID boss;
    private boolean bossCreated;
    private UUID worker;
    private UUID workerUser;
    private UUID project;
    private LocalDate today;

    @BeforeAll
    void fixtures() throws Exception {
        List<UUID> existing = jdbc.queryForList("select id from hr_employee where company_id = ? and user_id = ?", UUID.class, COMPANY_ID, ADMIN_USER);
        if (existing.isEmpty()) {
            boss = UUID.randomUUID();
            bossCreated = true;
            jdbc.update("insert into hr_employee(id, company_id, display_name, active, user_id) values (?, ?, 'TSH Notify Boss', true, ?)", boss, COMPANY_ID, ADMIN_USER);
        } else {
            boss = existing.get(0);
        }
        String username = "notif_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult created = mockMvc.perform(post("/api/v1/platform/users").header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"email\":\"" + username + "@example.local\",\"displayName\":\"Notify Worker\","
                                + "\"password\":\"initial-pass-1\"}"))
                .andExpect(status().isCreated()).andReturn();
        workerUser = UUID.fromString(body(created).get("id").asText());
        worker = UUID.randomUUID();
        jdbc.update("insert into hr_employee(id, company_id, display_name, active, user_id, manager_id) values (?, ?, 'TSH Notify Worker', true, ?, ?)",
                worker, COMPANY_ID, workerUser, boss);
        today = LocalDate.parse(body(mockMvc.perform(as(get(TS + "/me"))).andExpect(status().isOk()).andReturn()).get("today").asText());
        project = UUID.fromString(body(mockMvc.perform(as(post(TS + "/projects")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Notify " + UUID.randomUUID() + "\",\"billingMode\":\"HOURLY\"}")).andExpect(status().isOk()).andReturn()).get("id").asText());
    }

    @AfterAll
    void cleanUp() {
        jdbc.update("delete from tsh_reminder_log where employee_id in (?, ?)", worker, boss);
        jdbc.update("delete from tsh_week_posting where week_id in (select id from tsh_week where employee_id = ?)", worker);
        jdbc.update("delete from tsh_entry where employee_id = ?", worker);
        jdbc.update("delete from tsh_week where employee_id = ?", worker);
        jdbc.update("delete from hr_employee where id = ?", worker);
        if (bossCreated) {
            jdbc.update("delete from hr_employee where id = ?", boss);
        }
        jdbc.update("update tsh_company_settings set reminder_enabled = true, reminder_weekday = 'SUNDAY' where company_id = ?", COMPANY_ID);
    }

    private <T extends AbstractMockHttpServletRequestBuilder<T>> T as(T b) {
        return b.header("X-Company-Id", COMPANY_ID.toString()).header("X-User-Id", ADMIN_USER.toString());
    }

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private void logForWorker(LocalDate day, String duration) throws Exception {
        mockMvc.perform(as(post(TS + "/entries")).contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":\"" + worker
                + "\",\"workDate\":\"" + day + "\",\"projectId\":\"" + project + "\",\"duration\":\"" + duration + "\"}")).andExpect(status().isOk());
    }

    private UUID submitForWorker(LocalDate day) throws Exception {
        return UUID.fromString(body(mockMvc.perform(as(post(TS + "/weeks/submit")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"" + worker + "\",\"date\":\"" + day + "\"}")).andExpect(status().isOk()).andReturn()).get("weekId").asText());
    }

    /** The to-dos in a user's activities inbox for one model, "open" or "done". */
    private JsonNode inbox(UUID user, String model, String status) throws Exception {
        return body(mockMvc.perform(get("/api/v1/activities/inbox").header("X-Company-Id", COMPANY_ID.toString())
                        .param("companyId", COMPANY_ID.toString()).param("assigneeId", user.toString()).param("model", model)
                        .param("status", status).param("size", "100"))
                .andExpect(status().isOk()).andReturn()).get("content");
    }

    private JsonNode todoFor(JsonNode inbox, UUID record) {
        for (JsonNode a : inbox) {
            if (record.toString().equals(a.get("recordId").asText())) return a;
        }
        return null;
    }

    @Test
    void submissionReachesTheManagerAndTheDecisionClosesItAndInformsTheEmployee() throws Exception {
        LocalDate day = today.minusDays(190);
        logForWorker(day, "2h");
        UUID week = submitForWorker(day);

        JsonNode asked = todoFor(inbox(ADMIN_USER, "tsh.week", "open"), week);
        assertThat(asked).as("the manager has a to-do for the submitted week").isNotNull();
        assertThat(asked.get("subject").asText()).contains("Notify Worker").contains("to approve");
        assertThat(asked.get("kind").asText()).isEqualTo("ACTIVITY_TODO");

        // refusing closes the manager's to-do and tells the employee why
        mockMvc.perform(as(post(TS + "/weeks/" + week + "/refuse")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Tuesday is missing\"}")).andExpect(status().isOk());
        assertThat(todoFor(inbox(ADMIN_USER, "tsh.week", "open"), week)).isNull();
        JsonNode told = null;
        for (JsonNode a : inbox(workerUser, "tsh.week", "open")) {
            if (week.toString().equals(a.get("recordId").asText()) && a.get("subject").asText().startsWith("Timesheet refused")) told = a;
        }
        assertThat(told).as("the employee is told the week was refused").isNotNull();
        assertThat(told.get("body").asText()).contains("Tuesday is missing");

        // the employee fixes it and resubmits: the manager is asked again, and approving closes that to-do
        UUID again = submitForWorker(day);
        assertThat(again).isEqualTo(week);
        assertThat(todoFor(inbox(ADMIN_USER, "tsh.week", "open"), week)).isNotNull();
        mockMvc.perform(as(post(TS + "/weeks/" + week + "/approve"))).andExpect(status().isOk());
        assertThat(todoFor(inbox(ADMIN_USER, "tsh.week", "open"), week)).isNull();
        assertThat(todoFor(inbox(ADMIN_USER, "tsh.week", "done"), week)).as("kept as done, not deleted").isNotNull();

        // reopening an approved week informs the employee
        mockMvc.perform(as(post(TS + "/weeks/" + week + "/reopen")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"hours were wrong\"}")).andExpect(status().isOk());
        boolean reopenedNotice = false;
        for (JsonNode a : inbox(workerUser, "tsh.week", "open")) {
            if (a.get("subject").asText().startsWith("Timesheet reopened") && a.get("body").asText().contains("hours were wrong")) reopenedNotice = true;
        }
        assertThat(reopenedNotice).isTrue();
    }

    @Test
    void weeklyReminderGoesToTheEmployeeAndASummaryToTheManagerOncePerWeek() throws Exception {
        LocalDate lastWeek = today.minusDays(7);
        logForWorker(lastWeek, "3h");         // finished week, logged but never submitted

        JsonNode first = body(mockMvc.perform(as(post(TS + "/reminders/run"))).andExpect(status().isOk()).andReturn());
        assertThat(first.get("employeesReminded").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(first.get("managersNotified").asInt()).isGreaterThanOrEqualTo(1);

        JsonNode reminder = todoFor(inbox(workerUser, "tsh.reminder", "open"), worker);
        assertThat(reminder).as("the employee has a reminder").isNotNull();
        assertThat(reminder.get("subject").asText()).isEqualTo("Submit your timesheet");
        assertThat(reminder.get("body").asText()).contains("not submitted");

        JsonNode summary = todoFor(inbox(ADMIN_USER, "tsh.team-reminder", "open"), boss);
        assertThat(summary).as("the manager has a team summary").isNotNull();
        assertThat(summary.get("body").asText()).contains("Notify Worker (1 week)");

        // pressing it again the same week reminds nobody twice
        JsonNode second = body(mockMvc.perform(as(post(TS + "/reminders/run"))).andExpect(status().isOk()).andReturn());
        assertThat(second.get("employeesReminded").asInt()).isZero();
        assertThat(second.get("managersNotified").asInt()).isZero();
        int reminders = 0;
        for (JsonNode a : inbox(workerUser, "tsh.reminder", "all")) {
            if (worker.toString().equals(a.get("recordId").asText())) reminders++;
        }
        assertThat(reminders).isEqualTo(1);

        // once the week is submitted there is nothing left to remind about (next week's run)
        submitForWorker(lastWeek);
        jdbc.update("delete from tsh_reminder_log where employee_id in (?, ?)", worker, boss);
        JsonNode third = body(mockMvc.perform(as(post(TS + "/reminders/run"))).andExpect(status().isOk()).andReturn());
        int workerReminders = 0;
        for (JsonNode a : inbox(workerUser, "tsh.reminder", "all")) {
            if (worker.toString().equals(a.get("recordId").asText())) workerReminders++;
        }
        assertThat(workerReminders).as("still the one from before; the submitted week is not nagged again").isEqualTo(1);
        assertThat(third).isNotNull();
    }

    @Test
    void reminderOptionsAreSettingsAndRunningNeedsTheSettingsPermission() throws Exception {
        mockMvc.perform(as(put(TS + "/settings")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reminderEnabled\":false,\"reminderWeekday\":\"WEDNESDAY\"}"))
                .andExpect(status().isOk());
        JsonNode s = body(mockMvc.perform(as(get(TS + "/settings"))).andExpect(status().isOk()).andReturn());
        assertThat(s.get("reminderEnabled").asBoolean()).isFalse();
        assertThat(s.get("reminderWeekday").asText()).isEqualTo("WEDNESDAY");
        mockMvc.perform(as(put(TS + "/settings")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reminderEnabled\":true,\"reminderWeekday\":\"SUNDAY\"}")).andExpect(status().isOk());

        // the new worker has no timesheet permissions at all, so the run endpoint refuses them
        mockMvc.perform(post(TS + "/reminders/run").header("X-Company-Id", COMPANY_ID.toString()).header("X-User-Id", workerUser.toString()))
                .andExpect(status().isForbidden());
    }
}
