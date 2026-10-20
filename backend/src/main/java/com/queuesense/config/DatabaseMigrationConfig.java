package com.queuesense.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Bean
    public static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnMigration() {
        return new EntityManagerFactoryDependsOnPostProcessor("databaseSchemaMigration");
    }

    @Bean
    public DatabaseSchemaMigration databaseSchemaMigration(DataSource dataSource) {
        DatabaseSchemaMigration migration = new DatabaseSchemaMigration(dataSource);
        migration.migrate();
        return migration;
    }

    public static class DatabaseSchemaMigration {
        private final DataSource dataSource;

        public DatabaseSchemaMigration(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        public void migrate() {
            log.info("Checking database schema for required backward-compatible migrations...");
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                DatabaseMetaData metaData = conn.getMetaData();
                String productName = metaData.getDatabaseProductName();
                log.info("Database migration connected to: {}", productName);

                // Safe migration for 'resources' table
                if (tableExists(metaData, "resources")) {
                    if (!columnExists(metaData, "resources", "booking_closed")) {
                        log.info("Applying schema migration: Adding 'booking_closed' column to 'resources' table...");
                        stmt.execute("ALTER TABLE resources ADD COLUMN booking_closed BOOLEAN DEFAULT FALSE NOT NULL");
                        stmt.execute("UPDATE resources SET booking_closed = FALSE WHERE booking_closed IS NULL");
                        log.info("Successfully added 'booking_closed' column with default false.");
                    }

                    if (!columnExists(metaData, "resources", "booking_unit")) {
                        log.info("Applying schema migration: Adding 'booking_unit' column to 'resources' table...");
                        stmt.execute("ALTER TABLE resources ADD COLUMN booking_unit VARCHAR(100)");
                        log.info("Successfully added 'booking_unit' column.");
                    }

                    if (!columnExists(metaData, "resources", "closure_reason")) {
                        log.info("Applying schema migration: Adding 'closure_reason' column to 'resources' table...");
                        stmt.execute("ALTER TABLE resources ADD COLUMN closure_reason VARCHAR(255)");
                        log.info("Successfully added 'closure_reason' column.");
                    }
                }

                // Safe migration for 'usage_requests' table
                if (tableExists(metaData, "usage_requests")) {
                    if (!columnExists(metaData, "usage_requests", "quantity")) {
                        log.info("Applying schema migration: Adding 'quantity' column to 'usage_requests' table...");
                        stmt.execute("ALTER TABLE usage_requests ADD COLUMN quantity INTEGER DEFAULT 1 NOT NULL");
                        stmt.execute("UPDATE usage_requests SET quantity = 1 WHERE quantity IS NULL");
                        log.info("Successfully added 'quantity' column with default 1.");
                    }

                    if (!columnExists(metaData, "usage_requests", "slot_start_time")) {
                        log.info("Applying schema migration: Adding 'slot_start_time' column to 'usage_requests' table...");
                        stmt.execute("ALTER TABLE usage_requests ADD COLUMN slot_start_time TIMESTAMP");
                        log.info("Successfully added 'slot_start_time' column.");
                    }

                    if (!columnExists(metaData, "usage_requests", "slot_end_time")) {
                        log.info("Applying schema migration: Adding 'slot_end_time' column to 'usage_requests' table...");
                        stmt.execute("ALTER TABLE usage_requests ADD COLUMN slot_end_time TIMESTAMP");
                        log.info("Successfully added 'slot_end_time' column.");
                    }

                    if (!columnExists(metaData, "usage_requests", "resource_item_id")) {
                        log.info("Applying schema migration: Adding 'resource_item_id' column to 'usage_requests' table...");
                        stmt.execute("ALTER TABLE usage_requests ADD COLUMN resource_item_id BIGINT");
                        log.info("Successfully added 'resource_item_id' column.");
                    }
                }

                // Safe data migration for orphaned OCCUPIED resources (such as PC 04 from initial seed)
                if (tableExists(metaData, "resources") && tableExists(metaData, "facilities") && tableExists(metaData, "usage_requests")) {
                    log.info("Auditing and repairing orphaned OCCUPIED non-sports resources without active sessions...");
                    try {
                        int repaired = stmt.executeUpdate(
                                "UPDATE resources SET status = 'AVAILABLE' " +
                                "WHERE status = 'OCCUPIED' " +
                                "  AND id NOT IN (SELECT DISTINCT resource_id FROM usage_requests WHERE status = 'CHECKED_IN' AND resource_id IS NOT NULL) " +
                                "  AND facility_id IN (SELECT id FROM facilities WHERE UPPER(type) <> 'SPORTS')"
                        );
                        if (repaired > 0) {
                            log.info("Successfully repaired {} orphaned OCCUPIED resource(s) back to AVAILABLE.", repaired);
                        }
                    } catch (Exception ex) {
                        try {
                            int repaired = stmt.executeUpdate(
                                    "UPDATE \"resources\" SET \"status\" = 'AVAILABLE' " +
                                    "WHERE \"status\" = 'OCCUPIED' " +
                                    "  AND \"id\" NOT IN (SELECT DISTINCT \"resource_id\" FROM \"usage_requests\" WHERE \"status\" = 'CHECKED_IN' AND \"resource_id\" IS NOT NULL) " +
                                    "  AND \"facility_id\" IN (SELECT \"id\" FROM \"facilities\" WHERE UPPER(\"type\") <> 'SPORTS')"
                            );
                            if (repaired > 0) {
                                log.info("Successfully repaired {} orphaned OCCUPIED resource(s) back to AVAILABLE.", repaired);
                            }
                        } catch (Exception innerEx) {
                            log.warn("Could not execute orphaned OCCUPIED resources repair (safely ignored): {}", innerEx.getMessage());
                        }
                    }
                }

                log.info("Database schema migration verification completed.");
            } catch (Exception e) {
                log.error("Database schema migration error: {}", e.getMessage(), e);
                throw new RuntimeException("Database schema migration failed: " + e.getMessage(), e);
            }
        }

        private boolean tableExists(DatabaseMetaData metaData, String tableName) throws Exception {
            String[] testNames = {tableName, tableName.toUpperCase(), tableName.toLowerCase()};
            for (String name : testNames) {
                try (ResultSet rs = metaData.getTables(null, null, name, new String[]{"TABLE"})) {
                    if (rs.next()) return true;
                }
            }
            return false;
        }

        private boolean columnExists(DatabaseMetaData metaData, String tableName, String columnName) throws Exception {
            String[] testTables = {tableName, tableName.toUpperCase(), tableName.toLowerCase()};
            String[] testCols = {columnName, columnName.toUpperCase(), columnName.toLowerCase()};

            for (String t : testTables) {
                for (String c : testCols) {
                    try (ResultSet rs = metaData.getColumns(null, null, t, c)) {
                        if (rs.next()) return true;
                    }
                }
            }
            return false;
        }
    }
}
