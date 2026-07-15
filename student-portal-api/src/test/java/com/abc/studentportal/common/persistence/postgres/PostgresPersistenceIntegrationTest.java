package com.abc.studentportal.common.persistence.postgres;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.flywaydb.core.Flyway;
import com.abc.studentportal.common.migration.JdbcMigrationTracker;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the PostgreSQL/Flyway foundation. Repository behavior is covered in later checkpoints. */
@Tag("postgres-integration")
@Testcontainers
class PostgresPersistenceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("student_portal_test")
            .withUsername("student_portal")
            .withPassword("student_portal_test");

    static JdbcTemplate jdbc;

    @org.junit.jupiter.api.BeforeAll
    static void migrateSchema() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load().migrate();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void flywayCreatesRelationalSchema() {
        Integer departments = jdbc.queryForObject("select count(*) from departments", Integer.class);
        Integer enrollments = jdbc.queryForObject("select count(*) from enrollments", Integer.class);
        Integer migrationRuns = jdbc.queryForObject("select count(*) from migration_runs", Integer.class);
        assertThat(departments).isZero();
        assertThat(enrollments).isZero();
        assertThat(migrationRuns).isNotNegative();
    }

    @Test
    void persistsMigrationLifecycleAndCheckpoints() {
        JdbcMigrationTracker tracker = new JdbcMigrationTracker(jdbc);
        UUID runId = tracker.start(false, 25);

        tracker.checkpoint(runId, "STUDENT", 30, 25, 5, "opaque-cursor");
        tracker.complete(runId, false);

        assertThat(jdbc.queryForObject("select status from migration_runs where id = ?", String.class, runId))
                .isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("""
                select insert_count from migration_checkpoints
                where run_id = ? and entity_type = 'STUDENT'
                """, Long.class, runId)).isEqualTo(25L);
    }
}
