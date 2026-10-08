package com.bradox.erp.integration;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.rule.CostCalculator;
import com.bradox.erp.timesheet.service.domain.ports.output.LaborCostPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Hourly labor cost from the employee's running contract (TSH-07 #1): an hourly wage as is, a monthly wage
 * divided by the schedule's weekly hours times 52 over 12. Read only.
 */
@Component
public class TimesheetLaborCostAdapter implements LaborCostPort {

    private final NamedParameterJdbcTemplate jdbc;

    public TimesheetLaborCostAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Rate> hourlyRate(CompanyId companyId, UUID employeeId, LocalDate asOf) {
        MapSqlParameterSource p = new MapSqlParameterSource("c", companyId.getId()).addValue("e", employeeId)
                .addValue("d", Date.valueOf(asOf));
        List<Object[]> contracts = jdbc.query(
                "select wage, wage_type, currency_code, working_schedule_id from pay_contract "
                        + "where company_id = :c and employee_id = :e and state = 'running' "
                        + "and date_start <= :d and (date_end is null or date_end >= :d) order by date_start desc",
                p, (rs, i) -> new Object[]{rs.getBigDecimal("wage"), rs.getString("wage_type"),
                        rs.getString("currency_code"), rs.getObject("working_schedule_id", UUID.class)});
        if (contracts.isEmpty()) {
            return Optional.empty();
        }
        Object[] c = contracts.get(0);
        BigDecimal weeklyHours = jdbc.queryForObject(
                "select coalesce(sum(hours), 0) from pay_working_schedule_line where schedule_id = :s",
                new MapSqlParameterSource("s", c[3]), BigDecimal.class);
        int weeklyMinutes = weeklyHours == null ? 0 : weeklyHours.multiply(BigDecimal.valueOf(60)).intValue();
        BigDecimal rate = CostCalculator.hourlyRate((BigDecimal) c[0], (String) c[1], weeklyMinutes);
        return rate == null ? Optional.empty() : Optional.of(new Rate(rate, (String) c[2]));
    }
}
