package com.fairhire;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FairHireApplication.class)
@EnabledIf("isPostgresAvailable")
@TestPropertySource(properties = {
    "spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/fairhire_db}",
    "spring.datasource.username=postgres",
    "spring.datasource.password=postgres",
    "spring.datasource.driverClassName=org.postgresql.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "spring.flyway.baseline-on-migrate=true",
    "spring.flyway.locations=classpath:db/migration"
})
class PostgresSchemaValidationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    static boolean isPostgresAvailable() {
        String url = System.getenv().getOrDefault("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/fairhire_db");
        try (Connection conn = DriverManager.getConnection(url, "postgres", "postgres")) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testPostgresSchemaMigrationAndHibernateValidation() {
        // 1. Verify Flyway schema history table exists and has recorded migrations
        Integer flywayCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertNotNull(flywayCount);
        assertTrue(flywayCount >= 1, "Flyway migrations must be successfully recorded");

        // 2. Verify all 19 canonical tables exist in PostgreSQL 16
        List<String> expectedTables = List.of(
                "users", "jobs", "job_requirements", "candidates", "resumes",
                "skills", "job_skills", "resume_skills", "blind_screening_results",
                "bias_reports", "rewrite_suggestions", "match_results", "audit_logs",
                "dataset_versions", "dataset_records", "annotations",
                "experiment_runs", "experiment_results", "experiment_metrics"
        );

        for (String table : expectedTables) {
            Integer tableExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                    Integer.class,
                    table
            );
            assertEquals(1, tableExists, "Table '" + table + "' must exist in PostgreSQL public schema");
        }
    }
}
