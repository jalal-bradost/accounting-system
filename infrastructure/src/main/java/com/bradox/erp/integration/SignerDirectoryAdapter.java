package com.bradox.erp.integration;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.service.domain.ports.output.SignerDirectoryPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Names, emails and phones of contacts and users, read without importing those modules. Read only. */
@Component
public class SignerDirectoryAdapter implements SignerDirectoryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public SignerDirectoryAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Person> partner(CompanyId companyId, UUID partnerId) {
        List<Person> rows = jdbc.query("select id, display_name, email, phone from contacts_partner where company_id = :c and id = :id",
                new MapSqlParameterSource("c", companyId.getId()).addValue("id", partnerId),
                (rs, i) -> new Person(rs.getObject("id", UUID.class), rs.getString("display_name"), rs.getString("email"),
                        rs.getString("phone")));
        return rows.stream().findFirst();
    }

    @Override
    public Optional<Person> user(CompanyId companyId, UUID userId) {
        List<Person> rows = jdbc.query("select id, coalesce(display_name, username) as name, email from platform_app_user "
                        + "where company_id = :c and id = :id",
                new MapSqlParameterSource("c", companyId.getId()).addValue("id", userId),
                (rs, i) -> new Person(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("email"), null));
        return rows.stream().findFirst();
    }
}
