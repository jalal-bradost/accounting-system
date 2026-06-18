package com.jalaldeveloper.accountingsystem;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Utility: run with {@code mvn test -Dtest=SchemaExportTest#exportH2Ddl} to regenerate DDL from entities.
 */
@Disabled("Manual DDL export utility — run explicitly when regenerating Flyway baselines")
@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SchemaExportTest {

    @Autowired
    DataSource dataSource;

    @Test
    void exportH2Ddl() throws Exception {
        StringBuilder ddl = new StringBuilder();
        try (var conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SCRIPT NODATA")) {
            while (rs.next()) {
                ddl.append(rs.getString(1)).append(System.lineSeparator());
            }
        }
        Path out = Path.of("target", "generated-schema.sql");
        Files.createDirectories(out.getParent());
        Files.writeString(out, ddl.toString());
        System.out.println("Wrote DDL to " + out.toAbsolutePath() + " (" + ddl.length() + " chars)");
    }
}
