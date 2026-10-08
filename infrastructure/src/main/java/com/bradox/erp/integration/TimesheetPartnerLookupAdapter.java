package com.bradox.erp.integration;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.ports.output.PartnerLookupPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Customer names for timesheet projects, read from contacts without importing it. */
@Component
public class TimesheetPartnerLookupAdapter implements PartnerLookupPort {

    private final NamedParameterJdbcTemplate jdbc;

    public TimesheetPartnerLookupAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean exists(CompanyId companyId, UUID partnerId) {
        Integer n = jdbc.queryForObject("select count(*) from contacts_partner where company_id = :c and id = :id",
                new MapSqlParameterSource("c", companyId.getId()).addValue("id", partnerId), Integer.class);
        return n != null && n > 0;
    }

    @Override
    public Map<UUID, String> names(CompanyId companyId, Collection<UUID> ids) {
        Map<UUID, String> out = new HashMap<>();
        if (ids.isEmpty()) {
            return out;
        }
        jdbc.query("select id, display_name from contacts_partner where company_id = :c and id in (:ids)",
                new MapSqlParameterSource("c", companyId.getId()).addValue("ids", ids),
                rs -> {
                    out.put(rs.getObject("id", UUID.class), rs.getString("display_name"));
                });
        return out;
    }
}
