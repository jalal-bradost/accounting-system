package com.bradox.erp.integration;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.ports.output.ExpectedHoursPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Expected working time per day (TSH-03 #2, D9): the schedule of the employee's running contract,
 * minus public holidays and validated time off. Read only. Without a running contract every day is
 * {@code NO_SCHEDULE} with zero minutes, so the grid shows no false "gap".
 *
 * <p>The two-week calendar option of a schedule is not used yet; its lines are read as one week.
 */
@Component
public class TimesheetExpectedHoursAdapter implements ExpectedHoursPort {

    private record Contract(UUID scheduleId, LocalDate start, LocalDate end) {
        boolean covers(LocalDate d) {
            return !d.isBefore(start) && (end == null || !d.isAfter(end));
        }
    }

    private final NamedParameterJdbcTemplate jdbc;

    public TimesheetExpectedHoursAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<LocalDate, ExpectedDay> expected(CompanyId companyId, UUID employeeId, LocalDate from, LocalDate to) {
        MapSqlParameterSource p = new MapSqlParameterSource("c", companyId.getId()).addValue("e", employeeId)
                .addValue("from", Date.valueOf(from)).addValue("to", Date.valueOf(to));

        List<Contract> contracts = jdbc.query(
                "select working_schedule_id, date_start, date_end from pay_contract "
                        + "where company_id = :c and employee_id = :e and state = 'running' "
                        + "and date_start <= :to and (date_end is null or date_end >= :from) order by date_start desc",
                p, (rs, i) -> new Contract(rs.getObject("working_schedule_id", UUID.class),
                        rs.getDate("date_start").toLocalDate(),
                        rs.getDate("date_end") == null ? null : rs.getDate("date_end").toLocalDate()));

        Map<UUID, Map<Integer, Integer>> minutesBySchedule = new HashMap<>();
        for (Contract c : contracts) {
            minutesBySchedule.computeIfAbsent(c.scheduleId(), this::loadSchedule);
        }

        Map<LocalDate, String> holidays = new HashMap<>();
        jdbc.query("select holiday_date, name from hr_public_holiday where company_id = :c "
                        + "and holiday_date between :from and :to", p,
                rs -> {
                    holidays.put(rs.getDate("holiday_date").toLocalDate(), rs.getString("name"));
                });

        List<LocalDate[]> leave = jdbc.query("select date_from, date_to from hr_time_off_request where company_id = :c "
                        + "and employee_id = :e and state = 'validate' and date_from <= :to and date_to >= :from", p,
                (rs, i) -> new LocalDate[]{rs.getDate("date_from").toLocalDate(), rs.getDate("date_to").toLocalDate()});

        Map<LocalDate, ExpectedDay> out = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            final LocalDate day = d;
            Contract contract = contracts.stream().filter(c -> c.covers(day)).findFirst().orElse(null);
            if (contract == null) {
                out.put(d, new ExpectedDay(0, DayType.NO_SCHEDULE, null));
                continue;
            }
            int scheduled = minutesBySchedule.getOrDefault(contract.scheduleId(), Map.of())
                    .getOrDefault(d.getDayOfWeek().getValue(), 0);
            if (scheduled == 0) {
                out.put(d, new ExpectedDay(0, DayType.NON_WORKING, null));
            } else if (holidays.containsKey(d)) {
                out.put(d, new ExpectedDay(0, DayType.HOLIDAY, holidays.get(d)));
            } else if (leave.stream().anyMatch(r -> !day.isBefore(r[0]) && !day.isAfter(r[1]))) {
                out.put(d, new ExpectedDay(0, DayType.TIME_OFF, "Time off"));
            } else {
                out.put(d, new ExpectedDay(scheduled, DayType.WORKING, null));
            }
        }
        return out;
    }

    /** ISO day of week (1 = Monday) to scheduled minutes. */
    private Map<Integer, Integer> loadSchedule(UUID scheduleId) {
        Map<Integer, Integer> minutes = new HashMap<>();
        List<Object[]> rows = new ArrayList<>(jdbc.query(
                "select day_of_week, hours from pay_working_schedule_line where schedule_id = :s",
                new MapSqlParameterSource("s", scheduleId),
                (rs, i) -> new Object[]{rs.getInt("day_of_week"), rs.getBigDecimal("hours")}));
        for (Object[] r : rows) {
            int mins = ((BigDecimal) r[1]).multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).intValue();
            minutes.merge((Integer) r[0], mins, Integer::sum);
        }
        return minutes;
    }
}
