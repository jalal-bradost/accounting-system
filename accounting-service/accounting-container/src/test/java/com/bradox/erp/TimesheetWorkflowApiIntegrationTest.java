package com.bradox.erp;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.bootstrap.PlatformDefaultAdminUserSeeder;
import com.bradox.erp.platform.bootstrap.PlatformRbacSeeder;
import com.bradox.erp.timesheet.service.domain.ports.input.TimesheetTargetApplicationService;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
 * Timesheet workflow end to end over REST on the real Flyway schema: submit, approve, refuse, reopen, cost
 * snapshots, timer, team grid, reports, settings and record links. The admin user holds every permission.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimesheetWorkflowApiIntegrationTest {

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;
    private static final UUID USER_ID = PlatformDefaultAdminUserSeeder.DEFAULT_ADMIN_USER_ID;
    private static final String BASE = "/api/v1/timesheet";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TimesheetTargetApplicationService targets;

    private UUID adminEmployee;
    private UUID worker;
    private LocalDate today;
    private final UUID structure = UUID.randomUUID();
    private final UUID schedule = UUID.randomUUID();
    private final UUID contract = UUID.randomUUID();

    @BeforeAll
    void fixtures() throws Exception {
        List<UUID> existing = jdbc.queryForList("select id from hr_employee where company_id = ? and user_id = ?",
                UUID.class, COMPANY_ID, USER_ID);
        if (existing.isEmpty()) {
            adminEmployee = UUID.randomUUID();
            jdbc.update("insert into hr_employee(id, company_id, display_name, active, user_id) values (?, ?, 'TSH Admin', true, ?)",
                    adminEmployee, COMPANY_ID, USER_ID);
        } else {
            adminEmployee = existing.get(0);
        }
        worker = UUID.randomUUID();
        jdbc.update("insert into hr_employee(id, company_id, display_name, active) values (?, ?, 'TSH Worker', true)", worker, COMPANY_ID);
        jdbc.update("insert into pay_structure(id, company_id, name) values (?, ?, 'TSH wf')", structure, COMPANY_ID);
        jdbc.update("insert into pay_working_schedule(id, company_id, name) values (?, ?, 'TSH wf 5x8')", schedule, COMPANY_ID);
        for (int dow = 1; dow <= 5; dow++) {
            jdbc.update("insert into pay_working_schedule_line(id, schedule_id, day_of_week, hours) values (?, ?, ?, 8)",
                    UUID.randomUUID(), schedule, dow);
        }
        // An hourly wage of 10,000 IQD, so 2 hours cost 20,000.
        jdbc.update("insert into pay_contract(id, company_id, employee_id, name, structure_id, working_schedule_id, wage, wage_type, "
                        + "currency_code, date_start, state) values (?, ?, ?, 'TSH wf', ?, ?, 10000, 'hourly', 'IQD', DATE '2020-01-01', 'running')",
                contract, COMPANY_ID, worker, structure, schedule);
        today = LocalDate.parse(body(mockMvc.perform(as(get(BASE + "/me"))).andExpect(status().isOk()).andReturn()).get("today").asText());
    }

    @AfterAll
    void cleanUp() {
        for (UUID e : new UUID[]{adminEmployee, worker}) {
            jdbc.update("delete from tsh_entry where employee_id = ?", e);
            jdbc.update("delete from tsh_week where employee_id = ?", e);
            jdbc.update("delete from tsh_timer where employee_id = ?", e);
            jdbc.update("delete from tsh_grid_line where employee_id = ?", e);
        }
        jdbc.update("delete from pay_contract where id = ?", contract);
        jdbc.update("delete from pay_working_schedule_line where schedule_id = ?", schedule);
        jdbc.update("delete from pay_working_schedule where id = ?", schedule);
        jdbc.update("delete from pay_structure where id = ?", structure);
        jdbc.update("delete from hr_employee where id = ?", worker);
        jdbc.update("delete from hr_employee where id = ? and display_name = 'TSH Admin'", adminEmployee);
        jdbc.update("update tsh_company_settings set managers_may_self_approve = false, auto_lock_after_days = null, "
                + "rounding_step_minutes = 0, rounding_mode = 'NEAREST' where company_id = ?", COMPANY_ID);
    }

    private <T extends AbstractMockHttpServletRequestBuilder<T>> T as(T b) {
        return b.header("X-Company-Id", COMPANY_ID.toString()).header("X-User-Id", USER_ID.toString());
    }

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private UUID project(String name, String billingMode, boolean billable) throws Exception {
        MvcResult r = mockMvc.perform(as(post(BASE + "/projects")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + " " + UUID.randomUUID() + "\",\"billingMode\":\"" + billingMode
                                + "\",\"billableDefault\":" + billable + "}"))
                .andExpect(status().isOk()).andReturn();
        return UUID.fromString(body(r).get("id").asText());
    }

    private JsonNode logFor(UUID employee, LocalDate date, UUID project, UUID task, String duration) throws Exception {
        String payload = "{\"employeeId\":\"" + employee + "\",\"workDate\":\"" + date + "\",\"projectId\":"
                + (project == null ? "null" : "\"" + project + "\"") + ",\"taskId\":" + (task == null ? "null" : "\"" + task + "\"")
                + ",\"duration\":\"" + duration + "\"}";
        return body(mockMvc.perform(as(post(BASE + "/entries")).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andReturn());
    }

    private JsonNode submit(UUID employee, LocalDate date, int expectedStatus) throws Exception {
        return body(mockMvc.perform(as(post(BASE + "/weeks/submit")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"" + employee + "\",\"date\":\"" + date + "\"}")).andExpect(status().is(expectedStatus)).andReturn());
    }

    private JsonNode weekAction(UUID week, String action, String content, int expectedStatus) throws Exception {
        var req = as(post(BASE + "/weeks/" + week + "/" + action));
        if (content != null) req = req.contentType(MediaType.APPLICATION_JSON).content(content);
        return body(mockMvc.perform(req).andExpect(status().is(expectedStatus)).andReturn());
    }

    private JsonNode entriesOf(UUID employee, LocalDate day) throws Exception {
        return body(mockMvc.perform(as(get(BASE + "/entries")).param("employeeId", employee.toString())
                .param("from", day.toString()).param("to", day.toString())).andExpect(status().isOk()).andReturn());
    }

    @Test
    void submitApproveReopenAndRefuseWithCostSnapshots() throws Exception {
        UUID p = project("Weekflow", "HOURLY", true);
        // A Monday, so day and day + 1 are in the same week whatever the week start (Saturday by default).
        LocalDate day = today.minusDays(70).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        assertThat(logFor(worker, day, p, null, "2h").get("minutes").asInt()).isEqualTo(120);

        JsonNode submitted = submit(worker, day, 200);
        assertThat(submitted.get("status").asText()).isEqualTo("SUBMITTED");
        UUID week = UUID.fromString(submitted.get("weekId").asText());
        submit(worker, day, 422);                                      // cannot submit twice

        // the week is read only now
        mockMvc.perform(as(post(BASE + "/entries")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"" + worker + "\",\"workDate\":\"" + day + "\",\"projectId\":\"" + p + "\",\"minutes\":30}"))
                .andExpect(status().isUnprocessableEntity());

        // it shows in "to approve" for someone who may approve it
        MvcResult toApprove = mockMvc.perform(as(get(BASE + "/weeks")).param("scope", "APPROVE")).andExpect(status().isOk()).andReturn();
        boolean listed = false;
        for (JsonNode w : body(toApprove)) {
            if (week.toString().equals(w.get("weekId").asText())) {
                listed = true;
                assertThat(w.get("canApprove").asBoolean()).isTrue();
                assertThat(w.get("totalMinutes").asInt()).isEqualTo(120);
            }
        }
        assertThat(listed).isTrue();

        // refuse, then the employee fixes and resubmits
        weekAction(week, "refuse", "{\"reason\":\"Tuesday is missing\"}", 200);
        assertThat(body(mockMvc.perform(as(get(BASE + "/weeks/" + week))).andReturn()).get("refusedReason").asText()).isEqualTo("Tuesday is missing");
        weekAction(week, "refuse", "{\"reason\":\"again\"}", 422);     // only submitted weeks can be refused
        logFor(worker, day.plusDays(1), p, null, "1h");
        assertThat(submit(worker, day, 200).get("status").asText()).isEqualTo("SUBMITTED");

        JsonNode approved = weekAction(week, "approve", null, 200);
        assertThat(approved.get("status").asText()).isEqualTo("APPROVED");

        // cost snapshot: 10,000 per hour. 2 h = 20,000
        JsonNode first = entriesOf(worker, day).get("items").get(0);
        assertThat(first.get("costAmount").decimalValue().intValue()).isEqualTo(20000);
        assertThat(first.get("costMissing").asBoolean()).isFalse();
        assertThat(first.get("costCurrency").asText()).isEqualTo("IQD");

        // profitability sums the cost of the project
        mockMvc.perform(as(get(BASE + "/projects/" + p + "/profitability")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.loggedMinutes").value(180))
                .andExpect(jsonPath("$.laborCost").value(30000.0)).andExpect(jsonPath("$.missingCostEntries").value(0));

        // reopen needs a reason and clears the snapshot
        weekAction(week, "reopen", "{\"reason\":\"\"}", 400);
        assertThat(weekAction(week, "reopen", "{\"reason\":\"hours were wrong\"}", 200).get("status").asText()).isEqualTo("DRAFT");
        assertThat(entriesOf(worker, day).get("items").get(0).has("costAmount")).isFalse();
        weekAction(week, "reopen", "{\"reason\":\"twice\"}", 422);

        // bulk approve reports each week
        submit(worker, day, 200);
        MvcResult bulk = mockMvc.perform(as(post(BASE + "/weeks/bulk-approve")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"weekIds\":[\"" + week + "\",\"" + UUID.randomUUID() + "\"]}")).andExpect(status().isOk()).andReturn();
        assertThat(body(bulk).get("approved").asInt()).isEqualTo(1);
        assertThat(body(bulk).get("failed").asInt()).isEqualTo(1);
    }

    @Test
    void nobodyApprovesTheirOwnWeekUnlessTheCompanyAllowsIt() throws Exception {
        UUID p = project("Selfapprove", "HOURLY", false);
        LocalDate day = today.minusDays(80);
        logFor(adminEmployee, day, p, null, "1h");
        JsonNode s = submit(adminEmployee, day, 200);
        UUID week = UUID.fromString(s.get("weekId").asText());
        weekAction(week, "approve", null, 403);

        mockMvc.perform(as(put(BASE + "/settings")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managersMaySelfApprove\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.managersMaySelfApprove").value(true));
        weekAction(week, "approve", null, 200);
        mockMvc.perform(as(put(BASE + "/settings")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"managersMaySelfApprove\":false}")).andExpect(status().isOk());
    }

    @Test
    void missingContractFlagsCostAndRecomputeFixesItLater() throws Exception {
        UUID p = project("Nocost", "HOURLY", false);
        UUID noContract = UUID.randomUUID();
        jdbc.update("insert into hr_employee(id, company_id, display_name, active) values (?, ?, 'TSH NoContract', true)", noContract, COMPANY_ID);
        try {
            LocalDate day = today.minusDays(90);
            logFor(noContract, day, p, null, "1h");
            UUID week = UUID.fromString(submit(noContract, day, 200).get("weekId").asText());
            weekAction(week, "approve", null, 200);
            assertThat(entriesOf(noContract, day).get("items").get(0).get("costMissing").asBoolean()).isTrue();

            mockMvc.perform(as(post(BASE + "/entries/recompute-missing-cost"))).andExpect(status().isOk());
            jdbc.update("update pay_contract set employee_id = ? where id = ?", noContract, contract);   // give them the hourly contract
            MvcResult r = mockMvc.perform(as(post(BASE + "/entries/recompute-missing-cost"))).andExpect(status().isOk()).andReturn();
            assertThat(body(r).get("recomputed").asInt()).isGreaterThanOrEqualTo(1);
            JsonNode e = entriesOf(noContract, day).get("items").get(0);
            assertThat(e.get("costMissing").asBoolean()).isFalse();
            assertThat(e.get("costAmount").decimalValue().intValue()).isEqualTo(10000);
        } finally {
            jdbc.update("update pay_contract set employee_id = ? where id = ?", worker, contract);
            jdbc.update("delete from tsh_entry where employee_id = ?", noContract);
            jdbc.update("delete from tsh_week where employee_id = ?", noContract);
            jdbc.update("delete from hr_employee where id = ?", noContract);
        }
    }

    @Test
    void timerCannotRunTwiceAndTooShortRunsAreRefused() throws Exception {
        UUID p = project("Timer", "HOURLY", false);
        mockMvc.perform(as(get(BASE + "/timer"))).andExpect(status().isNoContent());
        mockMvc.perform(as(post(BASE + "/timer/start")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"" + p + "\",\"description\":\"on site\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("on site"));
        mockMvc.perform(as(post(BASE + "/timer/start")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\":\"" + p + "\"}")).andExpect(status().isUnprocessableEntity());
        mockMvc.perform(as(get(BASE + "/timer"))).andExpect(status().isOk()).andExpect(jsonPath("$.projectId").value(p.toString()));
        mockMvc.perform(as(post(BASE + "/timer/stop")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity());                // under a minute
        mockMvc.perform(as(delete(BASE + "/timer"))).andExpect(status().isNoContent());
        mockMvc.perform(as(get(BASE + "/timer"))).andExpect(status().isNoContent());
        mockMvc.perform(as(post(BASE + "/timer/stop")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity());                // nothing running
    }

    @Test
    void teamGridListsEveryoneForManagersAndTheTeamOtherwise() throws Exception {
        UUID p = project("Team", "HOURLY", false);
        LocalDate day = today.minusDays(100);
        logFor(worker, day, p, null, "3h");
        JsonNode all = body(mockMvc.perform(as(get(BASE + "/grid/team")).param("all", "true").param("date", day.toString()))
                .andExpect(status().isOk()).andReturn());
        assertThat(all.get("dates").size()).isEqualTo(7);
        JsonNode mine = null;
        for (JsonNode e : all.get("employees")) {
            if (worker.toString().equals(e.get("employeeId").asText())) mine = e;
        }
        assertThat(mine).isNotNull();
        assertThat(mine.get("totalMinutes").asInt()).isEqualTo(180);
        assertThat(mine.get("rows").get(0).get("totalMinutes").asInt()).isEqualTo(180);
        // the worker's contract expects 8 h on weekdays
        assertThat(mine.get("expectedMinutes").asInt()).isEqualTo(5 * 480);
        // team scope: the admin manages nobody here
        mockMvc.perform(as(get(BASE + "/grid/team")).param("date", day.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.employees.length()").value(0));
    }

    @Test
    void reportsGroupEveryWayAndExportCsv() throws Exception {
        UUID p = project("Reports", "HOURLY", true);
        LocalDate day = today.minusDays(110);
        logFor(worker, day, p, null, "4h");
        String from = day.minusDays(7).toString();
        String to = day.plusDays(7).toString();
        for (String group : List.of("EMPLOYEE", "PROJECT", "TASK", "CUSTOMER", "DEPARTMENT", "WEEK", "MONTH")) {
            MvcResult r = mockMvc.perform(as(get(BASE + "/reports/summary")).param("groupBy", group)
                    .param("from", from).param("to", to)).andReturn();
            assertThat(r.getResponse().getStatus()).as(group + " -> " + r.getResponse().getContentAsString()).isEqualTo(200);
            JsonNode rep = body(r);
            assertThat(rep.get("totalMinutes").asLong()).as(group).isEqualTo(240);
            assertThat(rep.get("billableMinutes").asLong()).as(group).isEqualTo(240);
            assertThat(rep.get("scope").asText()).isEqualTo("ALL");
        }
        JsonNode byProject = body(mockMvc.perform(as(get(BASE + "/reports/summary")).param("groupBy", "PROJECT")
                .param("from", from).param("to", to).param("billable", "true").param("status", "DRAFT")).andReturn());
        assertThat(byProject.get("rows").get(0).get("label").asText()).startsWith("Reports");
        assertThat(byProject.get("rows").get(0).get("minutes").asLong()).isEqualTo(240);
        assertThat(body(mockMvc.perform(as(get(BASE + "/reports/summary")).param("groupBy", "PROJECT")
                .param("from", from).param("to", to).param("status", "APPROVED")).andReturn()).get("totalMinutes").asLong()).isZero();

        MvcResult csv = mockMvc.perform(as(get(BASE + "/reports/export")).param("groupBy", "PROJECT").param("from", from).param("to", to))
                .andExpect(status().isOk()).andReturn();
        String text = csv.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(text).contains("project,hours,billable_hours,entries");
        assertThat(text).contains("Reports").contains("4.00");
        assertThat(csv.getResponse().getHeader("Content-Disposition")).contains("attachment");
    }

    @Test
    void dashboardAnswersForTheCurrentWeek() throws Exception {
        UUID p = project("Dash", "HOURLY", false);
        logFor(worker, today, p, null, "2h");
        mockMvc.perform(as(get(BASE + "/dashboard"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("ALL"))
                .andExpect(jsonPath("$.minutesThisWeek").isNumber())
                .andExpect(jsonPath("$.minutesThisMonth").isNumber())
                .andExpect(jsonPath("$.missingLastWeek").isArray())
                .andExpect(jsonPath("$.topProjects").isArray());
    }

    @Test
    void settingsValidateTheWorkflowOptions() throws Exception {
        mockMvc.perform(as(put(BASE + "/settings")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roundingStepMinutes\":15,\"roundingMode\":\"UP\",\"autoLockAfterDays\":30}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roundingStepMinutes").value(15))
                .andExpect(jsonPath("$.roundingMode").value("UP")).andExpect(jsonPath("$.autoLockAfterDays").value(30));
        mockMvc.perform(as(put(BASE + "/settings")).contentType(MediaType.APPLICATION_JSON).content("{\"roundingStepMinutes\":7}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(as(put(BASE + "/settings")).contentType(MediaType.APPLICATION_JSON).content("{\"autoLockAfterDays\":0}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(as(put(BASE + "/settings")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clearAutoLock\":true,\"roundingStepMinutes\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.autoLockAfterDays").doesNotExist());
    }

    @Test
    void recordLinkedTimeIsFoundByRecord() throws Exception {
        UUID record = UUID.randomUUID();
        UUID task = targets.ensureTask(new CompanyId(COMPANY_ID), "repair.order", record, "RO-IT-1", null);
        assertThat(targets.ensureTask(new CompanyId(COMPANY_ID), "repair.order", record, "RO-IT-1", null)).isEqualTo(task);
        logFor(adminEmployee, today.minusDays(120), null, task, "1h30");
        mockMvc.perform(as(get(BASE + "/entries/by-record")).param("model", "repair.order").param("recordId", record.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalMinutes").value(90))
                .andExpect(jsonPath("$.items[0].taskName").value("RO-IT-1"));
        targets.closeTask(new CompanyId(COMPANY_ID), "repair.order", record, false);
        mockMvc.perform(as(post(BASE + "/entries")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workDate\":\"" + today.minusDays(121) + "\",\"taskId\":\"" + task + "\",\"minutes\":30}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
