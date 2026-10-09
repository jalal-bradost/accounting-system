package com.bradox.erp.integration;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.service.domain.ports.output.RepairLookupPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Partner and product names for repair orders, read from contacts and inventory without importing them. */
@Component
public class RepairLookupAdapter implements RepairLookupPort {

    private final NamedParameterJdbcTemplate jdbc;

    public RepairLookupAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<UUID, String> partnerNames(CompanyId companyId, Collection<UUID> ids) {
        Map<UUID, String> out = new HashMap<>();
        jdbc.query("select id, display_name from contacts_partner where company_id = :c and id in (:ids)", params(companyId, ids),
                rs -> {
                    out.put(rs.getObject("id", UUID.class), rs.getString("display_name"));
                });
        return out;
    }

    /** "[SKU] Name", as product pickers show it. */
    @Override
    public Map<UUID, String> productNames(CompanyId companyId, Collection<UUID> ids) {
        Map<UUID, String> out = new HashMap<>();
        jdbc.query("select id, sku, name from inv_product where company_id = :c and id in (:ids)", params(companyId, ids),
                rs -> {
                    String sku = rs.getString("sku");
                    String name = rs.getString("name");
                    out.put(rs.getObject("id", UUID.class), sku == null || sku.isBlank() ? name : "[" + sku + "] " + name);
                });
        return out;
    }

    private static MapSqlParameterSource params(CompanyId companyId, Collection<UUID> ids) {
        return new MapSqlParameterSource("c", companyId.getId()).addValue("ids", ids);
    }
}
