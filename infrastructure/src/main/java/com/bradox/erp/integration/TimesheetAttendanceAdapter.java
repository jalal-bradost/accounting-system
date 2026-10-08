package com.bradox.erp.integration;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.rule.TimerSplit;
import com.bradox.erp.timesheet.service.domain.ports.output.AttendanceLookupPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Reads HR check-ins for the attendance hint (TSH-03 #11) without importing HR. Read only. */
@Component
public class TimesheetAttendanceAdapter implements AttendanceLookupPort {

    private final NamedParameterJdbcTemplate jdbc;

    public TimesheetAttendanceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<LocalDate, Integer> attendedMinutes(CompanyId companyId, UUID employeeId, LocalDate from, LocalDate to, ZoneId zone) {
        Instant windowStart = from.atStartOfDay(zone).toInstant();
        Instant windowEnd = to.plusDays(1).atStartOfDay(zone).toInstant();
        Map<LocalDate, Integer> out = new HashMap<>();
        // Records that overlap the window, completed ones only: a still-open check-in has no length yet.
        jdbc.query("select check_in, check_out from hr_attendance where company_id = :c and employee_id = :e "
                        + "and check_out is not null and check_in < :end and check_out > :start",
                new MapSqlParameterSource("c", companyId.getId()).addValue("e", employeeId)
                        .addValue("start", Timestamp.from(windowStart)).addValue("end", Timestamp.from(windowEnd)),
                rs -> {
                    Instant in = rs.getTimestamp("check_in").toInstant();
                    Instant outAt = rs.getTimestamp("check_out").toInstant();
                    for (TimerSplit.Piece piece : TimerSplit.split(in, outAt, zone)) {
                        if (!piece.date().isBefore(from) && !piece.date().isAfter(to)) {
                            out.merge(piece.date(), piece.minutes(), Integer::sum);
                        }
                    }
                });
        return out;
    }
}
