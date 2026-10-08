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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Timesheet end to end over REST on the real Flyway schema: projects, tasks, time entries and the weekly
 * grid, including expected hours read from HR (schedule, public holiday, time off).
 *
 * <p>The shared in-memory database is reused by other test classes, so this class links its own employee to
 * the default admin user, uses a different day per test, and removes what it created afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimesheetApiIntegrationTest {

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;
    private static final UUID USER_ID = PlatformDefaultAdminUserSeeder.DEFAULT_ADMIN_USER_ID;
    private static final String BASE = "/api/v1/timesheet";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID employeeId;
    private LocalDate today;

    @BeforeAll
    void linkAnEmployeeToTheAdminUser() throws Exception {
        List<UUID> existing = jdbc.queryForList("select id from hr_employee where company_id = ? and user_id = ?",
                UUID.class, COMPANY_ID, USER_ID);
        if (existing.isEmpty()) {
            employeeId = UUID.randomUUID();
            jdbc.update("insert into hr_employee(id, company_id, display_name, active, user_id) values (?, ?, ?, true, ?)",
                    employeeId, COMPANY_ID, "TSH Tester", USER_ID);
        } else {
            employeeId = existing.get(0);
        }
        today = LocalDate.parse(body(mockMvc.perform(as(get(BASE + "/me"))).andExpect(status().isOk()).andReturn())
                .get("today").asText());
    }

    @AfterAll
    void cleanUp() {
        jdbc.update("delete from tsh_entry where employee_id = ?", employeeId);
        jdbc.update("delete from tsh_grid_line where employee_id = ?", employeeId);
        jdbc.update("delete from tsh_week where employee_id = ?", employeeId);
        jdbc.update("delete from tsh_task_assignee where employee_id = ?", employeeId);
        jdbc.update("delete from hr_employee where id = ? and display_name = 'TSH Tester'", employeeId);
    }

    private <T extends AbstractMockHttpServletRequestBuilder<T>> T as(T b) {
        return b.header("X-Company-Id", COMPANY_ID.toString()).header("X-User-Id", USER_ID.toString());
    }

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private UUID createProject(String name, String code, String billingMode, boolean billable) throws Exception {
        String payload = "{\"name\":\"" + name + "\"," + (code == null ? "" : "\"code\":\"" + code + "\",")
                + "\"billingMode\":\"" + billingMode + "\",\"billableDefault\":" + billable + ",\"allocatedMinutes\":600}";
        MvcResult r = mockMvc.perform(as(post(BASE + "/projects")).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andReturn();
        return UUID.fromString(body(r).get("id").asText());
    }

    private JsonNode logEntry(LocalDate date, UUID project, UUID task, String duration, int expectedStatus) throws Exception {
        String payload = "{\"workDate\":\"" + date + "\",\"projectId\":" + (project == null ? "null" : "\"" + project + "\"")
                + ",\"taskId\":" + (task == null ? "null" : "\"" + task + "\"") + ",\"duration\":\"" + duration + "\"}";
        MvcResult r = mockMvc.perform(as(post(BASE + "/entries")).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().is(expectedStatus)).andReturn();
        return body(r);
    }

    private JsonNode setCell(UUID project, UUID task, LocalDate date, String duration, int expectedStatus) throws Exception {
        String payload = "{\"projectId\":\"" + project + "\"," + (task == null ? "" : "\"taskId\":\"" + task + "\",")
                + "\"workDate\":\"" + date + "\",\"duration\":\"" + duration + "\"}";
        return body(mockMvc.perform(as(put(BASE + "/grid/cell")).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().is(expectedStatus)).andReturn());
    }

    private JsonNode grid(LocalDate date) throws Exception {
        return body(mockMvc.perform(as(get(BASE + "/grid")).param("date", date.toString()))
                .andExpect(status().isOk()).andReturn());
    }

    private JsonNode rowFor(JsonNode grid, UUID project, UUID task) {
        for (JsonNode row : grid.get("rows")) {
            boolean sameTask = task == null ? row.get("taskId").isNull() : task.toString().equals(row.get("taskId").asText());
            if (project.toString().equals(row.get("projectId").asText()) && sameTask) {
                return row;
            }
        }
        return null;
    }

    @Test
    void meShowsTheLinkedEmployeeAndCompanyDefaults() throws Exception {
        mockMvc.perform(as(get(BASE + "/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.id").value(employeeId.toString()))
                .andExpect(jsonPath("$.settings.weekStartDay").value("SATURDAY"))
                .andExpect(jsonPath("$.settings.timezone").value("Asia/Baghdad"))
                .andExpect(jsonPath("$.canManageProjects").value(true));
    }

    @Test
    void internalProjectIsSeededOnceAndProjectCodesAreUnique() throws Exception {
        for (int i = 0; i < 2; i++) {
            MvcResult r = mockMvc.perform(as(get(BASE + "/projects"))).andExpect(status().isOk()).andReturn();
            long internal = 0;
            for (JsonNode p : body(r)) {
                if (p.get("internal").asBoolean()) {
                    internal++;
                    assertThat(p.get("name").asText()).isEqualTo("Internal");
                    assertThat(p.get("billableDefault").asBoolean()).isFalse();
                }
            }
            assertThat(internal).isEqualTo(1);
        }
        String code = "UQ" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        createProject("Code owner", code, "HOURLY", false);
        mockMvc.perform(as(post(BASE + "/projects")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other\",\"code\":\"" + code.toLowerCase() + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void fixedPriceProjectsAreNeverBillableByDefault() throws Exception {
        UUID id = createProject("Fixed job " + UUID.randomUUID(), null, "FIXED_PRICE", true);
        mockMvc.perform(as(get(BASE + "/projects/" + id)))
                .andExpect(jsonPath("$.billingMode").value("FIXED_PRICE"))
                .andExpect(jsonPath("$.billableDefault").value(false));
        LocalDate day = today.minusDays(40);
        mockMvc.perform(as(post(BASE + "/entries")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workDate\":\"" + day + "\",\"projectId\":\"" + id + "\",\"minutes\":60,\"billable\":true}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void tasksMoveThroughStatusesAndClosedTasksTakeNoTime() throws Exception {
        UUID project = createProject("Tasks " + UUID.randomUUID(), null, "HOURLY", false);
        MvcResult created = mockMvc.perform(as(post(BASE + "/tasks")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"" + project + "\",\"name\":\"Design\",\"allocatedMinutes\":120,"
                                + "\"assigneeEmployeeIds\":[\"" + employeeId + "\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.assigneeNames[0]").value("TSH Tester")).andReturn();
        UUID task = UUID.fromString(body(created).get("id").asText());

        LocalDate day = today.minusDays(41);
        JsonNode entry = logEntry(day, null, task, "30m", 200);
        assertThat(entry.get("projectId").asText()).isEqualTo(project.toString());

        mockMvc.perform(as(get(BASE + "/projects/" + project + "/tasks")))
                .andExpect(jsonPath("$[0].loggedMinutes").value(30));

        mockMvc.perform(as(post(BASE + "/tasks/" + task + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.acceptsEntries").value(false));
        logEntry(day, null, task, "30m", 422);
    }

    @Test
    void logListEditAndDeleteAnEntry() throws Exception {
        UUID project = createProject("Billable " + UUID.randomUUID(), null, "HOURLY", true);
        LocalDate day = today.minusDays(42);
        JsonNode created = logEntry(day, project, null, "1h30", 200);
        UUID id = UUID.fromString(created.get("id").asText());
        assertThat(created.get("minutes").asInt()).isEqualTo(90);
        assertThat(created.get("billable").asBoolean()).isTrue();
        assertThat(created.get("source").asText()).isEqualTo("MANUAL");
        assertThat(created.get("employeeId").asText()).isEqualTo(employeeId.toString());

        mockMvc.perform(as(get(BASE + "/entries")).param("from", day.toString()).param("to", day.toString())
                        .param("projectId", project.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalMinutes").value(90))
                .andExpect(jsonPath("$.billableMinutes").value(90));

        mockMvc.perform(as(put(BASE + "/entries/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minutes\":120,\"description\":\"fixed wording\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minutes").value(120))
                .andExpect(jsonPath("$.description").value("fixed wording"))
                .andExpect(jsonPath("$.projectId").value(project.toString()));

        mockMvc.perform(as(delete(BASE + "/entries/" + id))).andExpect(status().isNoContent());
        mockMvc.perform(as(get(BASE + "/entries/" + id))).andExpect(status().isNotFound());
    }

    @Test
    void dayTotalsWarnPast12HoursAndRejectPast24() throws Exception {
        UUID project = createProject("Long day " + UUID.randomUUID(), null, "HOURLY", false);
        LocalDate day = today.minusDays(43);
        assertThat(logEntry(day, project, null, "12h", 200).get("warning").isNull()).isTrue();
        assertThat(logEntry(day, project, null, "1m", 200).get("warning").asText()).contains("12 hours");
        logEntry(day, project, null, "11h59m", 200);                     // the day is now exactly 24 h
        logEntry(day, project, null, "1m", 422);
        logEntry(day, project, null, "abc", 422);
    }

    @Test
    void futureDatesAndArchivedProjectsAreRefused() throws Exception {
        UUID project = createProject("Archive me " + UUID.randomUUID(), null, "HOURLY", false);
        logEntry(today.plusDays(1), project, null, "1h", 422);
        mockMvc.perform(as(post(BASE + "/projects/" + project + "/archive")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));
        logEntry(today.minusDays(44), project, null, "1h", 422);
        mockMvc.perform(as(post(BASE + "/projects/" + project + "/unarchive"))).andExpect(status().isOk());
        logEntry(today.minusDays(44), project, null, "1h", 200);
    }

    @Test
    void typingIntoGridCellsCreatesChangesAndClearsEntries() throws Exception {
        UUID project = createProject("Grid " + UUID.randomUUID(), null, "HOURLY", false);
        LocalDate day = today.minusDays(45);

        assertThat(setCell(project, null, day, "2h", 200).get("minutes").asInt()).isEqualTo(120);
        JsonNode row = rowFor(grid(day), project, null);
        assertThat(row).isNotNull();
        assertThat(row.get("totalMinutes").asInt()).isEqualTo(120);

        assertThat(setCell(project, null, day, "2:30", 200).get("minutes").asInt()).isEqualTo(150);
        MvcResult list = mockMvc.perform(as(get(BASE + "/entries")).param("from", day.toString()).param("to", day.toString())
                .param("projectId", project.toString())).andReturn();
        assertThat(body(list).get("items").size()).isEqualTo(1);

        assertThat(setCell(project, null, day, "", 200).get("minutes").asInt()).isZero();
        assertThat(rowFor(grid(day), project, null)).isNull();
    }

    @Test
    void aCellHoldingSeveralEntriesCannotBeOverwrittenFromTheGrid() throws Exception {
        UUID project = createProject("Multi " + UUID.randomUUID(), null, "HOURLY", false);
        LocalDate day = today.minusDays(46);
        logEntry(day, project, null, "1h", 200);
        logEntry(day, project, null, "30m", 200);
        JsonNode cell = null;
        for (JsonNode c : rowFor(grid(day), project, null).get("cells")) {
            if (day.toString().equals(c.get("date").asText())) {
                cell = c;
            }
        }
        assertThat(cell).isNotNull();
        assertThat(cell.get("minutes").asInt()).isEqualTo(90);
        assertThat(cell.get("entryCount").asInt()).isEqualTo(2);
        setCell(project, null, day, "5h", 422);
    }

    @Test
    void addedLinesStayEvenWhenEmptyAndCopyForwardFromLastWeek() throws Exception {
        UUID project = createProject("Lines " + UUID.randomUUID(), null, "HOURLY", false);
        LocalDate day = today.minusDays(47);
        mockMvc.perform(as(post(BASE + "/grid/lines")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\":\"" + project + "\"}")).andExpect(status().isNoContent());
        JsonNode row = rowFor(grid(day), project, null);
        assertThat(row).isNotNull();
        assertThat(row.get("pinned").asBoolean()).isTrue();
        assertThat(row.get("totalMinutes").asInt()).isZero();

        mockMvc.perform(as(post(BASE + "/grid/lines/remove")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\":\"" + project + "\"}")).andExpect(status().isNoContent());
        assertThat(rowFor(grid(day), project, null)).isNull();

        UUID worked = createProject("Last week " + UUID.randomUUID(), null, "HOURLY", false);
        LocalDate lastWeek = today.minusDays(55);
        logEntry(lastWeek, worked, null, "1h", 200);
        mockMvc.perform(as(post(BASE + "/grid/copy-last-week")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weekStart\":\"" + lastWeek.plusDays(7) + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.added").value(1));
        assertThat(rowFor(grid(lastWeek.plusDays(7)), worked, null)).isNotNull();
    }

    @Test
    void expectedHoursComeFromTheScheduleHolidaysAndTimeOff() throws Exception {
        LocalDate anchor = today.minusDays(60);
        JsonNode before = grid(anchor);
        LocalDate start = LocalDate.parse(before.get("weekStart").asText());
        assertThat(before.get("days").get(2).get("dayType").asText()).isEqualTo("NO_SCHEDULE");
        assertThat(before.get("expectedMinutes").asInt()).isZero();

        UUID structure = UUID.randomUUID();
        UUID schedule = UUID.randomUUID();
        UUID contract = UUID.randomUUID();
        UUID leaveType = UUID.randomUUID();
        UUID holiday = UUID.randomUUID();
        UUID leave = UUID.randomUUID();
        try {
            jdbc.update("insert into pay_structure(id, company_id, name) values (?, ?, 'TSH test')", structure, COMPANY_ID);
            jdbc.update("insert into pay_working_schedule(id, company_id, name) values (?, ?, 'TSH Mon-Fri')", schedule, COMPANY_ID);
            for (int dow = 1; dow <= 5; dow++) {
                jdbc.update("insert into pay_working_schedule_line(id, schedule_id, day_of_week, hours) values (?, ?, ?, 8)",
                        UUID.randomUUID(), schedule, dow);
            }
            jdbc.update("insert into pay_contract(id, company_id, employee_id, name, structure_id, working_schedule_id, wage, "
                            + "currency_code, date_start, state) values (?, ?, ?, 'TSH', ?, ?, 1000, 'IQD', ?, 'running')",
                    contract, COMPANY_ID, employeeId, structure, schedule, java.sql.Date.valueOf(start.minusDays(30)));
            jdbc.update("insert into hr_public_holiday(id, company_id, name, holiday_date) values (?, ?, 'Test Holiday', ?)",
                    holiday, COMPANY_ID, java.sql.Date.valueOf(start.plusDays(3)));
            jdbc.update("insert into hr_time_off_type(id, company_id, name, code, display_code) values (?, ?, 'TSH leave', ?, 'TSH')",
                    leaveType, COMPANY_ID, "TSH" + UUID.randomUUID().toString().substring(0, 8));
            jdbc.update("insert into hr_time_off_request(id, company_id, employee_id, time_off_type_id, date_from, date_to, "
                            + "number_of_days, state) values (?, ?, ?, ?, ?, ?, 1, 'validate')",
                    leave, COMPANY_ID, employeeId, leaveType, java.sql.Date.valueOf(start.plusDays(4)),
                    java.sql.Date.valueOf(start.plusDays(4)));

            JsonNode days = grid(anchor).get("days");
            // Saturday-start week: Sat, Sun, Mon, Tue, Wed, Thu, Fri. A Mon-Fri schedule leaves Sat and Sun off.
            assertThat(days.get(0).get("dayType").asText()).isEqualTo("NON_WORKING");
            assertThat(days.get(1).get("dayType").asText()).isEqualTo("NON_WORKING");
            assertThat(days.get(2).get("dayType").asText()).isEqualTo("WORKING");        // Monday
            assertThat(days.get(2).get("expectedMinutes").asInt()).isEqualTo(480);
            assertThat(days.get(3).get("dayType").asText()).isEqualTo("HOLIDAY");        // Tuesday
            assertThat(days.get(3).get("dayLabel").asText()).isEqualTo("Test Holiday");
            assertThat(days.get(3).get("expectedMinutes").asInt()).isZero();
            assertThat(days.get(4).get("dayType").asText()).isEqualTo("TIME_OFF");       // Wednesday
            assertThat(days.get(4).get("expectedMinutes").asInt()).isZero();
            assertThat(days.get(5).get("dayType").asText()).isEqualTo("WORKING");        // Thursday
            assertThat(days.get(6).get("dayType").asText()).isEqualTo("WORKING");        // Friday
            assertThat(grid(anchor).get("expectedMinutes").asInt()).isEqualTo(3 * 480);
        } finally {
            jdbc.update("delete from hr_time_off_request where id = ?", leave);
            jdbc.update("delete from hr_time_off_type where id = ?", leaveType);
            jdbc.update("delete from hr_public_holiday where id = ?", holiday);
            jdbc.update("delete from pay_contract where id = ?", contract);
            jdbc.update("delete from pay_working_schedule_line where schedule_id = ?", schedule);
            jdbc.update("delete from pay_working_schedule where id = ?", schedule);
            jdbc.update("delete from pay_structure where id = ?", structure);
        }
    }
}
