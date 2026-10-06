package com.bradox.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FlywayMigrationIntegrationTest {

    @Autowired
    DataSource dataSource;

    @Test
    void flywayBaselineCreatesCoreTables() throws Exception {
        Set<String> tables = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }
        assertTrue(tables.contains("accounts"));
        assertTrue(tables.contains("platform_company"));
        assertTrue(tables.contains("contacts_partner"));
        assertTrue(tables.contains("hr_employee"));
        assertTrue(tables.contains("hr_department"));
        assertTrue(tables.contains("hr_attendance"));
        assertTrue(tables.contains("inv_product"));
        assertTrue(tables.contains("sal_sales_order"));
        assertTrue(tables.contains("pur_purchase_order"));
        assertTrue(tables.contains("pos_order"));
        assertTrue(tables.contains("acc_customer_payment_allocation"));
        assertTrue(tables.contains("pur_vendor_payment_allocation"));
        assertTrue(tables.contains("inv_stock_hold"));
        assertTrue(tables.contains("flyway_schema_history"));
    }
}
