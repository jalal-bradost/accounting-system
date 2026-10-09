package com.bradox.erp.integration;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.exception.RepairDomainException;
import com.bradox.erp.repair.service.domain.ports.output.SaleQuotationPort;
import com.bradox.erp.sales.service.domain.dto.CreateSalesOrderCommand;
import com.bradox.erp.sales.service.domain.dto.SalesOrderLineCommand;
import com.bradox.erp.sales.service.domain.dto.SalesOrderResponse;
import com.bradox.erp.sales.service.domain.ports.input.SalesApplicationService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Creates the draft sale quotation of a confirmed repair order, without Repair importing Sales. */
@Component
public class RepairSaleQuotationAdapter implements SaleQuotationPort {

    private final SalesApplicationService sales;
    private final CompanyCurrencyAdapter currencies;
    private final NamedParameterJdbcTemplate jdbc;

    public RepairSaleQuotationAdapter(SalesApplicationService sales, CompanyCurrencyAdapter currencies,
                                      NamedParameterJdbcTemplate jdbc) {
        this.sales = sales;
        this.currencies = currencies;
        this.jdbc = jdbc;
    }

    @Override
    public Quotation create(CompanyId companyId, UUID customerPartnerId, String note, List<Line> lines) {
        UUID company = companyId.getId();
        CreateSalesOrderCommand cmd = new CreateSalesOrderCommand();
        cmd.setCompanyId(company);
        cmd.setCustomerPartnerId(customerPartnerId);
        cmd.setCurrencyCode(currencies.defaultCurrencyCode(companyId));
        cmd.setWarehouseId(jdbc.query("select id from inv_warehouse where company_id = :c and active = true order by code",
                new MapSqlParameterSource("c", company), (rs, i) -> rs.getObject("id", UUID.class)).stream().findFirst().orElse(null));
        cmd.setNotes(note);
        List<SalesOrderLineCommand> out = new ArrayList<>();
        for (Line l : lines) {
            ProductRow p = jdbc.query("select name, uom_id from inv_product where company_id = :c and id = :id",
                            new MapSqlParameterSource("c", company).addValue("id", l.productId()),
                            (rs, i) -> new ProductRow(rs.getString("name"), rs.getObject("uom_id", UUID.class)))
                    .stream().findFirst()
                    .orElseThrow(() -> new RepairDomainException("error.repair.partNotFound", null, "A part's product no longer exists"));
            SalesOrderLineCommand lc = new SalesOrderLineCommand();
            lc.setProductId(l.productId());
            lc.setName(p.name());
            lc.setUomId(p.uomId());
            lc.setQtyOrdered(l.qty());
            out.add(lc);
        }
        cmd.setLines(out);
        SalesOrderResponse created = sales.createSalesOrder(cmd);
        return new Quotation(created.getId(), created.getName());
    }

    private record ProductRow(String name, UUID uomId) {
    }
}
