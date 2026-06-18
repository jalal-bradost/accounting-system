package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V6__PartnerImage extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        if (!tableExists(conn, "CONTACTS_PARTNER") && !tableExists(conn, "contacts_partner")) {
            return;
        }
        String table = tableExists(conn, "contacts_partner") ? "contacts_partner" : "CONTACTS_PARTNER";
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
