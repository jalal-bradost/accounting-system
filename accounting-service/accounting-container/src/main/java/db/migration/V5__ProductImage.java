package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Adds product image columns when {@code inv_product} already exists (e.g. PostgreSQL with
 * {@code ddl-auto=validate}). Skipped on fresh H2 dev where Flyway runs before Hibernate
 * creates inventory tables — Hibernate maps {@code imageUrl} from {@link
 * com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.ProductEntity} instead.
 */
public class V5__ProductImage extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        if (!tableExists(conn, "INV_PRODUCT") && !tableExists(conn, "inv_product")) {
            return;
        }
        String table = tableExists(conn, "inv_product") ? "inv_product" : "INV_PRODUCT";
        addColumnIfMissing(conn, table, "image_url", "VARCHAR(512)");
        addColumnIfMissing(conn, table, "image_content_type", "VARCHAR(100)");
    }

    private static boolean tableExists(Connection conn, String table) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        String catalog = conn.getCatalog();
        try (ResultSet rs = meta.getTables(catalog, null, table, new String[] {"TABLE"})) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getTables(catalog, null, table.toUpperCase(), new String[] {"TABLE"})) {
            return rs.next();
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String ddlType)
            throws Exception {
        if (columnExists(conn, table, column)) return;
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddlType);
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        String catalog = conn.getCatalog();
        for (String col : new String[] {column, column.toUpperCase()}) {
            try (ResultSet rs = meta.getColumns(catalog, null, table, col)) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = meta.getColumns(catalog, null, table.toUpperCase(), col)) {
                if (rs.next()) return true;
            }
        }
        return false;
    }
}
