package com.bradox.erp.integration;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lets Timesheet read HR employees (and the user to employee link, D12) without importing HR.
 * Read only; the SQL is fixed, never built from request input.
 */
@Component
public class TimesheetEmployeeLookupAdapter implements EmployeeLookupPort {

    private static final String SELECT = "select id, display_name, manager_id, department_id from hr_employee ";

    private static final RowMapper<EmployeeRef> MAPPER = (rs, i) -> new EmployeeRef(
            rs.getObject("id", UUID.class), rs.getString("display_name"),
            rs.getObject("manager_id", UUID.class), rs.getObject("department_id", UUID.class));

    private final NamedParameterJdbcTemplate jdbc;

    public TimesheetEmployeeLookupAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<EmployeeRef> findByUser(CompanyId companyId, UUID userId) {
        return jdbc.query(SELECT + "where company_id = :c and user_id = :u and active = true",
                new MapSqlParameterSource("c", companyId.getId()).addValue("u", userId), MAPPER).stream().findFirst();
    }

    @Override
    public Optional<EmployeeRef> find(CompanyId companyId, UUID employeeId) {
        return jdbc.query(SELECT + "where company_id = :c and id = :id",
                new MapSqlParameterSource("c", companyId.getId()).addValue("id", employeeId), MAPPER).stream().findFirst();
    }

    @Override
    public Map<UUID, EmployeeRef> findAll(CompanyId companyId, Collection<UUID> ids) {
        Map<UUID, EmployeeRef> out = new LinkedHashMap<>();
        if (ids.isEmpty()) {
            return out;
        }
        jdbc.query(SELECT + "where company_id = :c and id in (:ids)",
                new MapSqlParameterSource("c", companyId.getId()).addValue("ids", ids), MAPPER)
                .forEach(e -> out.put(e.id(), e));
        return out;
    }

    @Override
    public Optional<UUID> departmentManager(CompanyId companyId, UUID departmentId) {
        return jdbc.query("select manager_id from hr_department where company_id = :c and id = :id",
                new MapSqlParameterSource("c", companyId.getId()).addValue("id", departmentId),
                (rs, i) -> rs.getObject("manager_id", UUID.class)).stream().filter(java.util.Objects::nonNull).findFirst();
    }

    @Override
    public Map<UUID, String> departmentNames(CompanyId companyId, Collection<UUID> departmentIds) {
        Map<UUID, String> out = new LinkedHashMap<>();
        if (departmentIds.isEmpty()) {
            return out;
        }
        jdbc.query("select id, name from hr_department where company_id = :c and id in (:ids)",
                new MapSqlParameterSource("c", companyId.getId()).addValue("ids", departmentIds),
                rs -> {
                    out.put(rs.getObject("id", UUID.class), rs.getString("name"));
                });
        return out;
    }

    @Override
    public List<EmployeeRef> findTeam(CompanyId companyId, UUID managerEmployeeId) {
        return jdbc.query(SELECT + "where company_id = :c and active = true and id <> :m and (manager_id = :m "
                        + "or department_id in (select id from hr_department where company_id = :c and manager_id = :m))",
                new MapSqlParameterSource("c", companyId.getId()).addValue("m", managerEmployeeId), MAPPER);
    }

    @Override
    public Map<UUID, UUID> userIds(CompanyId companyId, Collection<UUID> employeeIds) {
        Map<UUID, UUID> out = new LinkedHashMap<>();
        if (employeeIds.isEmpty()) {
            return out;
        }
        jdbc.query("select id, user_id from hr_employee where company_id = :c and user_id is not null and id in (:ids)",
                new MapSqlParameterSource("c", companyId.getId()).addValue("ids", employeeIds),
                rs -> {
                    out.put(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class));
                });
        return out;
    }

    @Override
    public List<EmployeeRef> listActive(CompanyId companyId) {
        return jdbc.query(SELECT + "where company_id = :c and active = true order by display_name",
                new MapSqlParameterSource("c", companyId.getId()), MAPPER);
    }
}
