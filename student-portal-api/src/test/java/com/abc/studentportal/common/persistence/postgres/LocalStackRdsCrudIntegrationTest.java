package com.abc.studentportal.common.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.domain.Department;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Tag("localstack-rds")
@SpringBootTest
@ActiveProfiles("local-postgres")
class LocalStackRdsCrudIntegrationTest {
    @Autowired DepartmentRepository departments;

    @Test
    void departmentCrudRoundTripUsesLocalStackRds() {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        Department created = departments.create(new Department(id, "RDS-" + id.toString().substring(0, 8),
                "RDS Integration", "LocalStack RDS", now, now, 0));
        assertThat(departments.findById(id)).contains(created);
        assertThat(departments.findByCode(created.code())).contains(created);
        departments.delete(created);
        assertThat(departments.findById(id)).isEmpty();
    }
}
