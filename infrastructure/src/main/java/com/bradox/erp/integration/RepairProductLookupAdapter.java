package com.bradox.erp.integration;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.service.domain.ports.output.ProductLookupPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Product name and list price for repair lines, read from inventory without importing it. */
@Component
public class RepairProductLookupAdapter implements ProductLookupPort {

    private final NamedParameterJdbcTemplate jdbc;

    public RepairProductLookupAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Product> find(CompanyId companyId, UUID productId) {
        List<Product> found = jdbc.query("select id, name, list_price from inv_product where company_id = :c and id = :id and active = true",
                new MapSqlParameterSource("c", companyId.getId()).addValue("id", productId),
                (rs, i) -> new Product(rs.getObject("id", UUID.class), rs.getString("name"), rs.getBigDecimal("list_price")));
        return found.stream().findFirst();
    }
}
