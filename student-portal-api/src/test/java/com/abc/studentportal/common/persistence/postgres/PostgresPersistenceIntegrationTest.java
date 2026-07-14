package com.abc.studentportal.common.persistence.postgres;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.flywaydb.core.Flyway;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the PostgreSQL/Flyway foundation. Repository behavior is covered in later checkpoints. */
@Tag("postgres-integration")
@Testcontainers
@SpringBootTest
@ActiveProfiles("test-postgres")
class PostgresPersistenceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("student_portal_test")
            .withUsername("student_portal")
            .withPassword("student_portal_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @org.junit.jupiter.api.BeforeAll
    static void migrateSchema() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load().migrate();
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayCreatesRelationalSchema() {
        Integer departments = jdbc.queryForObject("select count(*) from departments", Integer.class);
        Integer enrollments = jdbc.queryForObject("select count(*) from enrollments", Integer.class);
        assertThat(departments).isZero();
        assertThat(enrollments).isZero();
    }
}
