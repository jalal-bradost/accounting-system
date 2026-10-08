package com.bradox.erp;

import com.bradox.erp.platform.bootstrap.PlatformDefaultAdminUserSeeder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TSH-06 billing and TSH-10 ledger posting across the real modules: a Sales order line invoiced from timesheets, the
 * Accounting ledger with the project dimension, and the reopen rules that protect both.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimesheetBillingPostingIntegrationTest extends SalesScenarioSupport {

    private static final UUID USER_ID = PlatformDefaultAdminUserSeeder.DEFAULT_ADMIN_USER_ID;
    private static final String TS = "/api/v1/timesheet";

    @Autowired
    private JdbcTemplate jdbc;

    private UUID adminEmployee;
    private UUID worker;
    private LocalDate today;
    private final UUID structure = UUID.randomUUID();
    private final UUID schedule = UUID.randomUUID();
    private final UUID contract = UUID.randomUUID();

    @BeforeAll
    void fixtures() throws Exception {
        List<UUID> existing = jdbc.queryForList("select id from hr_employee where company_id = ? and user_id = ?", UUID.class, COMPANY_ID, USER_ID);
        if (existing.isEmpty()) {
            adminEmployee = UUID.randomUUID();
            jdbc.update("insert into hr_employee(id, company_id, display_name, active, user_id) values (?, ?, 'TSH Billing Admin', true, ?)",
                    adminEmployee, COMPANY_ID, USER_ID);
        } else {
            adminEmployee = existing.get(0);
        }
        worker = UUID.randomUUID();
        jdbc.update("insert into hr_employee(id, company_id, display_name, active) values (?, ?, 'TSH Billing Worker', true)", worker, COMPANY_ID);
        jdbc.update("insert into pay_structure(id, company_id, name) values (?, ?, 'TSH bill')", structure, COMPANY_ID);
        jdbc.update("insert into pay_working_schedule(id, company_id, name) values (?, ?, 'TSH bill 5x8')", schedule, COMPANY_ID);
        for (int dow = 1; dow <= 5; dow++) {
            jdbc.update("insert into pay_working_schedule_line(id, schedule_id, day_of_week, hours) values (?, ?, ?, 8)", UUID.randomUUID(), schedule, dow);
        }
        // An hourly wage of 10,000 in the company's base currency, so cost needs no conversion in these assertions.
        String base = jdbc.queryForObject("select code from company_currencies where company_id = ? and base_currency = true and active = true",
                String.class, COMPANY_ID);
        jdbc.update("insert into pay_contract(id, company_id, employee_id, name, structure_id, working_schedule_id, wage, wage_type, "
                + "currency_code, date_start, state) values (?, ?, ?, 'TSH bill', ?, ?, 10000, 'hourly', ?, DATE '2020-01-01', 'running')",
                contract, COMPANY_ID, worker, structure, schedule, base);
        today = LocalDate.parse(ts(get(TS + "/me")).andExpect(status().isOk()).json().get("today").asText());
    }

    @AfterAll
    void cleanUp() {
        for (UUID e : new UUID[]{adminEmployee, worker}) {
            jdbc.update("delete from tsh_week_posting_line where posting_id in (select p.id from tsh_week_posting p "
                    + "join tsh_week w on w.id = p.week_id where w.employee_id = ?)", e);
            jdbc.update("delete from tsh_week_posting where week_id in (select id from tsh_week where employee_id = ?)", e);
            jdbc.update("delete from tsh_entry where employee_id = ?", e);
            jdbc.update("delete from tsh_week where employee_id = ?", e);
        }
        jdbc.update("delete from pay_contract where id = ?", contract);
        jdbc.update("delete from pay_working_schedule_line where schedule_id = ?", schedule);
        jdbc.update("delete from pay_working_schedule where id = ?", schedule);
        jdbc.update("delete from pay_structure where id = ?", structure);
        jdbc.update("delete from hr_employee where id = ?", worker);
        jdbc.update("delete from hr_employee where id = ? and display_name = 'TSH Billing Admin'", adminEmployee);
        jdbc.update("update tsh_company_settings set managers_may_self_approve = false, ledger_posting_enabled = false where company_id = ?", COMPANY_ID);
    }

    /** A timesheet call acts as the admin user, who is linked to an employee and holds every permission. */
    private Result ts(MockHttpServletRequestBuilder request) throws Exception {
        return callAs(request, USER_ID);
    }

    private JsonNode timesheetOrder(UUID product, int qty) throws Exception {
        String body = "{\"customerPartnerId\":\"" + customer + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse + "\",\"lines\":["
                + "{\"productId\":\"" + product + "\",\"name\":\"Consulting hours\",\"uomId\":\"" + uomId + "\",\"qtyOrdered\":" + qty
                + ",\"unitPrice\":100,\"discountPercent\":0,\"invoicePolicy\":\"TIMESHEET\",\"taxIds\":[]}]}";
        JsonNode so = call(post("/api/v1/sales/orders").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).json();
        return confirm(so);
    }

    private UUID project(String name, UUID defaultLine) throws Exception {
        String line = defaultLine == null ? "" : ",\"defaultSaleLineId\":\"" + defaultLine + "\"";
        return ts(post(TS + "/projects").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + " " + UUID.randomUUID() + "\",\"billingMode\":\"HOURLY\",\"billableDefault\":true" + line + "}"))
                .andExpect(status().isOk()).id();
    }

    private JsonNode setLedger(boolean on) throws Exception {
        return ts(put(TS + "/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ledgerPostingEnabled\":" + on + ",\"managersMaySelfApprove\":true}")).andExpect(status().isOk()).json();
    }

    private UUID logWorker(LocalDate day, UUID project, String duration) throws Exception {
        return ts(post(TS + "/entries").contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":\"" + worker + "\",\"workDate\":\""
                + day + "\",\"projectId\":\"" + project + "\",\"duration\":\"" + duration + "\"}")).andExpect(status().isOk()).id();
    }

    private UUID submitWorker(LocalDate day) throws Exception {
        return UUID.fromString(ts(post(TS + "/weeks/submit").contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"" + worker + "\",\"date\":\"" + day + "\"}")).andExpect(status().isOk()).json().get("weekId").asText());
    }

    private JsonNode approve(UUID week) throws Exception {
        return ts(post(TS + "/weeks/" + week + "/approve")).andExpect(status().isOk()).json();
    }

    private BigDecimal delivered(JsonNode so) throws Exception {
        return reload(so).get("lines").get(0).get("qtyDelivered").decimalValue();
    }

    private BigDecimal balanceFor(UUID account, UUID project, LocalDate from, LocalDate to) throws Exception {
        JsonNode tb = ts(get("/api/v1/companies/" + COMPANY_ID + "/trial-balance").param("from", from.toString()).param("to", to.toString())
                .param("analyticModel", "tsh.project").param("analyticId", project.toString())).andExpect(status().isOk()).json();
        for (JsonNode l : tb.get("lines")) {
            if (account.toString().equals(l.get("accountId").asText())) {
                return l.get("balance").decimalValue();
            }
        }
        return BigDecimal.ZERO;
    }

    @Test
    void approvedHoursBecomeDeliveredQuantityAndLedgerCostPerProject() throws Exception {
        JsonNode so = timesheetOrder(serviceProduct(), 10);
        UUID line = UUID.fromString(lineId(so, 0));
        UUID project = project("Billed", line);
        JsonNode settings = setLedger(true);
        assertThat(settings.get("ledgerPostingEnabled").asBoolean()).isTrue();
        assertThat(settings.get("journalCode").asText()).isEqualTo("TSH");
        UUID costAccount = UUID.fromString(settings.get("defaultCostAccountId").asText());
        UUID appliedAccount = UUID.fromString(settings.get("laborAppliedAccountId").asText());

        LocalDate day1 = today.minusDays(130);
        logWorker(day1, project, "2h30");
        assertThat(delivered(so)).as("draft hours deliver nothing").isEqualByComparingTo("0");
        UUID week1 = submitWorker(day1);
        assertThat(delivered(so)).as("submitted is still not approved").isEqualByComparingTo("0");
        approve(week1);

        // TSH-06: 150 minutes of approved billable time become 2.50 delivered hours on the order line
        assertThat(delivered(so)).isEqualByComparingTo("2.5");
        assertThat(reload(so).get("invoiceStatus").asText()).isEqualTo("TO_INVOICE");

        // TSH-10: one posted entry, 10,000 an hour, debited to the cost account against the project
        JsonNode postings = ts(get(TS + "/postings").param("status", "POSTED")).andExpect(status().isOk()).json();
        JsonNode mine = null;
        for (JsonNode p : postings) {
            if (week1.toString().equals(p.get("weekId").asText())) mine = p;
        }
        assertThat(mine).isNotNull();
        assertThat(mine.get("totalAmount").decimalValue()).isEqualByComparingTo("25000");
        assertThat(mine.get("journalEntryId").isNull()).isFalse();
        assertThat(mine.get("latePosted").asBoolean()).isFalse();
        assertThat(mine.get("lines").get(0).get("projectId").asText()).isEqualTo(project.toString());
        assertThat(balanceFor(costAccount, project, day1.minusDays(10), today)).isEqualByComparingTo("25000");
        assertThat(balanceFor(appliedAccount, project, day1.minusDays(10), today)).as("credit goes to 'Labor cost applied' without a project").isEqualByComparingTo("0");
        JsonNode gl = ts(get("/api/v1/companies/" + COMPANY_ID + "/general-ledger").param("from", day1.minusDays(10).toString())
                .param("to", today.toString()).param("analyticModel", "tsh.project").param("analyticId", project.toString()))
                .andExpect(status().isOk()).json();
        assertThat(gl.get("lines").size()).as("only the project's own debit line").isEqualTo(1);

        // the order is invoiced like any delivered line
        invoiceAll(so);
        JsonNode invoiced = reload(so);
        assertThat(invoiced.get("lines").get(0).get("qtyInvoiced").decimalValue()).isEqualByComparingTo("2.5");
        assertThat(invoiced.get("invoiceStatus").asText()).isEqualTo("FULL");

        // BR-TSH-14: invoiced hours cannot be taken away by reopening the week
        ts(post(TS + "/weeks/" + week1 + "/reopen").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"typo\"}"))
                .refusedWith("invoiced");
        assertThat(delivered(so)).isEqualByComparingTo("2.5");

        // a second week adds to the quantity; reopening it removes only its own hours and reverses only its own posting
        LocalDate day2 = today.minusDays(137);
        logWorker(day2, project, "1h");
        UUID week2 = submitWorker(day2);
        approve(week2);
        assertThat(delivered(so)).isEqualByComparingTo("3.5");
        assertThat(balanceFor(costAccount, project, day2.minusDays(10), today)).isEqualByComparingTo("35000");

        ts(post(TS + "/weeks/" + week2 + "/reopen").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"wrong day\"}"))
                .andExpect(status().isOk());
        assertThat(delivered(so)).isEqualByComparingTo("2.5");
        assertThat(balanceFor(costAccount, project, day2.minusDays(10), today)).as("the reversal nets the second week to zero").isEqualByComparingTo("25000");
        JsonNode reversed = ts(get(TS + "/postings").param("status", "REVERSED")).andExpect(status().isOk()).json();
        boolean sawReversal = false;
        for (JsonNode p : reversed) {
            if (week2.toString().equals(p.get("weekId").asText())) {
                sawReversal = true;
                assertThat(p.get("reversalEntryId").isNull()).isFalse();
            }
        }
        assertThat(sawReversal).isTrue();

        // re-approval makes version 2 and still leaves exactly one active posting for the week
        UUID again = submitWorker(day2);
        assertThat(again).isEqualTo(week2);
        approve(week2);
        int active = 0;
        for (JsonNode p : ts(get(TS + "/postings")).andExpect(status().isOk()).json()) {
            if (week2.toString().equals(p.get("weekId").asText()) && !"REVERSED".equals(p.get("status").asText())) {
                active++;
                assertThat(p.get("version").asInt()).isEqualTo(2);
            }
        }
        assertThat(active).isEqualTo(1);
    }

    @Test
    void weekWithoutAnOrderLineNeedsAttentionUntilALineIsChosen() throws Exception {
        UUID project = project("No line", null);
        LocalDate day = today.minusDays(150);
        UUID entry = logWorker(day, project, "2h");
        approve(submitWorker(day));

        JsonNode attention = ts(get(TS + "/billing/needs-attention")).andExpect(status().isOk()).json();
        JsonNode item = null;
        for (JsonNode i : attention) {
            if (entry.toString().equals(i.get("entryId").asText())) item = i;
        }
        assertThat(item).isNotNull();
        assertThat(item.get("reason").asText()).isEqualTo("NO_LINE");

        JsonNode so = timesheetOrder(serviceProduct(), 8);
        assertThat(delivered(so)).isEqualByComparingTo("0");
        ts(post(TS + "/entries/" + entry + "/assign-line").contentType(MediaType.APPLICATION_JSON)
                .content("{\"saleLineId\":\"" + lineId(so, 0) + "\"}")).andExpect(status().isOk());
        assertThat(delivered(so)).isEqualByComparingTo("2");
        for (JsonNode i : ts(get(TS + "/billing/needs-attention")).andExpect(status().isOk()).json()) {
            assertThat(i.get("entryId").asText()).isNotEqualTo(entry.toString());
        }
    }

    @Test
    void onlyTimesheetServiceLinesCanBeBilledFromTimesheets() throws Exception {
        // a stocked product cannot take the timesheet policy
        UUID stocked = stockedProduct(5);
        String body = "{\"customerPartnerId\":\"" + customer + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse + "\",\"lines\":["
                + "{\"productId\":\"" + stocked + "\",\"name\":\"Goods\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":1,\"unitPrice\":10,\"discountPercent\":0,\"invoicePolicy\":\"TIMESHEET\",\"taxIds\":[]}]}";
        call(post("/api/v1/sales/orders").contentType(MediaType.APPLICATION_JSON).content(body)).refusedWith("service");

        // a normal service line is not offered, a timesheet line is
        JsonNode so = timesheetOrder(serviceProduct(), 3);
        UUID eligible = UUID.fromString(lineId(so, 0));
        boolean listed = false;
        for (JsonNode l : ts(get(TS + "/billing/sale-lines")).andExpect(status().isOk()).json()) {
            if (eligible.toString().equals(l.get("lineId").asText())) listed = true;
        }
        assertThat(listed).isTrue();
        UUID plain = serviceProduct();
        JsonNode ordinary = createOrder(line(null, plain, 1, 10));
        confirm(ordinary);
        UUID ordinaryLine = UUID.fromString(lineId(reload(ordinary), 0));
        ts(post(TS + "/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Bad line " + UUID.randomUUID()
                + "\",\"billingMode\":\"HOURLY\",\"defaultSaleLineId\":\"" + ordinaryLine + "\"}")).refusedWith("timesheets");
    }

    @Test
    void stockRecomputeNeverWipesTimesheetDeliveredQuantity() throws Exception {
        UUID service = serviceProduct();
        UUID goods = stockedProduct(20);
        String body = "{\"customerPartnerId\":\"" + customer + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse + "\",\"lines\":["
                + "{\"productId\":\"" + service + "\",\"name\":\"Hours\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":5,\"unitPrice\":100,\"discountPercent\":0,\"invoicePolicy\":\"TIMESHEET\",\"taxIds\":[]},"
                + "{\"productId\":\"" + goods + "\",\"name\":\"Goods\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":2,\"unitPrice\":10,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode so = confirm(call(post("/api/v1/sales/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).json());
        UUID project = project("Stock safe", UUID.fromString(lineId(so, 0)));
        LocalDate day = today.minusDays(160);
        logWorker(day, project, "3h");
        approve(submitWorker(day));
        assertThat(delivered(so)).isEqualByComparingTo("3");

        deliverAll(so);   // validating the stock delivery recomputes the delivered quantity of every line of the order
        JsonNode after = reload(so);
        assertThat(after.get("lines").get(1).get("qtyDelivered").decimalValue()).as("the goods were delivered").isEqualByComparingTo("2");
        assertThat(after.get("lines").get(0).get("qtyDelivered").decimalValue()).as("the stock recompute left the timesheet line alone")
                .isEqualByComparingTo("3");
    }

    @Test
    void backfillPostsWeeksApprovedBeforePostingWasOn() throws Exception {
        setLedger(false);
        UUID project = project("Backfill", null);
        LocalDate day = today.minusDays(170);
        logWorker(day, project, "1h");
        UUID week = submitWorker(day);
        approve(week);
        for (JsonNode p : ts(get(TS + "/postings")).andExpect(status().isOk()).json()) {
            assertThat(p.get("weekId").asText()).isNotEqualTo(week.toString());
        }
        setLedger(true);
        JsonNode result = ts(post(TS + "/postings/backfill")).andExpect(status().isOk()).json();
        JsonNode item = null;
        for (JsonNode i : result.get("items")) {
            if (week.toString().equals(i.get("weekId").asText())) item = i;
        }
        assertThat(item).isNotNull();
        assertThat(item.get("status").asText()).isEqualTo("POSTED");
        // nothing left to do the second time
        for (JsonNode i : ts(post(TS + "/postings/backfill")).andExpect(status().isOk()).json().get("items")) {
            assertThat(i.get("weekId").asText()).isNotEqualTo(week.toString());
        }
    }

    @Test
    void projectRatesLetEmployeesBillAtDifferentPrices() throws Exception {
        JsonNode senior = timesheetOrder(serviceProduct(), 20);
        JsonNode junior = timesheetOrder(serviceProduct(), 20);
        UUID project = project("Two prices", UUID.fromString(lineId(junior, 0)));
        ts(put(TS + "/projects/" + project + "/rates").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rates\":[{\"employeeId\":\"" + worker + "\",\"saleLineId\":\"" + lineId(senior, 0) + "\"}]}"))
                .andExpect(status().isOk());
        assertThat(ts(get(TS + "/projects/" + project + "/rates")).andExpect(status().isOk()).json().get(0).get("employeeName").asText())
                .isEqualTo("TSH Billing Worker");
        LocalDate day = today.minusDays(180);
        logWorker(day, project, "4h");
        approve(submitWorker(day));
        assertThat(delivered(senior)).as("the worker's own rate line").isEqualByComparingTo("4");
        assertThat(delivered(junior)).as("the project default is not used for this worker").isEqualByComparingTo("0");
    }
}
