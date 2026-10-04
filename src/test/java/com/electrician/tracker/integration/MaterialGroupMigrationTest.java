package com.electrician.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/**
 * V17__material_group.sql on a database that already holds material lines
 * from before groups existed: each line takes its product's category, lines
 * of products without a category stay ungrouped.
 */
class MaterialGroupMigrationTest {

    @Test
    void existingLinesTakeTheirProductsCategory() throws Exception {
        Path file = Files.createTempFile("v17-migration", ".db");
        file.toFile().deleteOnExit();
        String url = "jdbc:sqlite:" + file;
        flyway(url, "16").migrate();
        try (Connection connection = DriverManager.getConnection(url); Statement sql = connection.createStatement()) {
            sql.execute("INSERT INTO customer (id, name) VALUES (1, 'Ahmet Bey')");
            sql.execute("INSERT INTO job (id, customer_id, type, name, status) VALUES (1, 1, 'SITE', 'Blok A', 'ACTIVE')");
            sql.execute("INSERT INTO product (id, name, unit, category) VALUES (1, 'Kablo', 'METER', 'Kablo')");
            sql.execute("INSERT INTO product (id, name, unit, category) VALUES (2, 'Röle', 'PIECE', NULL)");
            sql.execute("INSERT INTO material_item (id, job_id, product_id, quantity, sale_unit_price) "
                    + "VALUES (1, 1, 1, '10', '5'), (2, 1, 2, '1', '50')");
        }

        flyway(url, null).migrate();

        assertThat(groupsByLine(url)).containsExactly(Map.entry(1L, "Kablo"), Map.entry(2L, "<null>"));
    }

    private static Flyway flyway(String url, String target) {
        var configuration = Flyway.configure().dataSource(url, null, null).locations("classpath:db/migration");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private static Map<Long, String> groupsByLine(String url) throws SQLException {
        Map<Long, String> groups = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection(url); Statement sql = connection.createStatement();
                ResultSet rows = sql.executeQuery("SELECT id, group_name FROM material_item ORDER BY id")) {
            while (rows.next()) {
                String group = rows.getString("group_name");
                groups.put(rows.getLong("id"), group == null ? "<null>" : group);
            }
        }
        return groups;
    }
}
