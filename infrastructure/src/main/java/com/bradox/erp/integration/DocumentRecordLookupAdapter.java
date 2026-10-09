package com.bradox.erp.integration;

import com.bradox.erp.documents.service.domain.ports.output.RecordLookupPort;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lets Documents link to records of other modules without importing them. Tables and label
 * columns come from a fixed list below, never from request input, so the SQL is safe. Add a line
 * to {@link #MODELS} to make another record type linkable.
 */
@Component
public class DocumentRecordLookupAdapter implements RecordLookupPort {

    private record Model(String table, String labelColumn, String readPermission) {
    }

    private static final Map<String, Model> MODELS = Map.of(
            "contacts.partner", new Model("contacts_partner", "display_name", "contacts.partner.read"),
            "hr.employee", new Model("hr_employee", "display_name", "hr.employee.read"),
            "accounting.customer-invoice", new Model("acc_customer_invoice", "reference", "accounting.customer-invoice.read"),
            "purchase.vendor-bill", new Model("pur_vendor_bill", "reference", "purchase.vendor-bill.read"),
            "sales.order", new Model("sal_sales_order", "name", "sales.order.read"),
            "purchase.order", new Model("pur_purchase_order", "name", "purchase.order.read"),
            "repair.order", new Model("rep_order", "reference", "rep.order.view"),
            "project.task", new Model("prj_task", "name", "project.view"),
            "inventory.product", new Model("inv_product", "name", "inventory.product.read"));

    private final JdbcTemplate jdbc;

    public DocumentRecordLookupAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean supports(String modelName) {
        return MODELS.containsKey(modelName);
    }

    @Override
    public boolean exists(CompanyId companyId, String modelName, UUID recordId) {
        Model m = MODELS.get(modelName);
        if (m == null) {
            return false;
        }
        Integer count = jdbc.queryForObject(
                "select count(*) from " + m.table() + " where id = ? and company_id = ?",
                Integer.class, recordId, companyId.getId());
        return count != null && count > 0;
    }

    @Override
    public Optional<String> label(CompanyId companyId, String modelName, UUID recordId) {
        Model m = MODELS.get(modelName);
        if (m == null) {
            return Optional.empty();
        }
        try {
            List<String> rows = jdbc.query(
                    "select " + m.labelColumn() + " from " + m.table() + " where id = ? and company_id = ?",
                    (rs, i) -> rs.getString(1), recordId, companyId.getId());
            if (rows.isEmpty()) {
                return Optional.empty();
            }
            String label = rows.get(0);
            return Optional.of(label == null || label.isBlank() ? fallbackLabel(modelName, recordId) : label);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> readPermission(String modelName) {
        Model m = MODELS.get(modelName);
        return m == null ? Optional.empty() : Optional.of(m.readPermission());
    }

    private static String fallbackLabel(String modelName, UUID recordId) {
        return modelName + " " + recordId.toString().substring(0, 8);
    }
}
