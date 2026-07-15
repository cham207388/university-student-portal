package com.abc.studentportal.common.persistence.postgres;

import com.abc.studentportal.common.application.StudentCourseQueries;
import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.application.CourseQueries;
import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.domain.*;
import com.abc.studentportal.department.application.*;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.enrollment.application.PostgresEnrollmentService;
import com.abc.studentportal.enrollment.application.EnrollmentQueries;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.domain.Instructor;
import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.domain.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("postgres-integration")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-postgres")
class PostgresApiIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("student_portal_api_test").withUsername("student_portal").withPassword("student_portal_test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired MockMvc mvc;
    @Autowired DepartmentRepository departments;
    @Autowired StudentRepository students;
    @Autowired InstructorRepository instructors;
    @Autowired CourseRepository courses;
    @Autowired DepartmentQueries departmentQueries;
    @Autowired CourseQueries courseQueries;
    @Autowired StudentCourseQueries relationships;
    @Autowired PostgresEnrollmentService enrollmentService;
    @Autowired EnrollmentQueries enrollmentQueries;
    @Autowired JdbcTemplate jdbc;

    @Test
    void postgresProfileStartsControllersAndPaginatesCollections() throws Exception {
        Instant now = Instant.now();
        Department first = department("PG-A", now);
        Department second = department("PG-B", now);

        var firstPage = departmentQueries.findAll(new CursorRequest(1, null));
        assertThat(firstPage.content()).hasSize(1);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(departmentQueries.findAll(new CursorRequest(1, firstPage.nextCursor())).content()).hasSize(1);

        mvc.perform(get("/api/v1/departments").param("limit", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/v1/departments").param("unsupported", "value"))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void enrollmentEnforcesCapacityAndRelationshipQueries() {
        Instant now = Instant.now();
        Department department = department("PG-ENR", now);
        Instructor instructor = instructors.create(new Instructor(UUID.randomUUID(), "EMP-" + UUID.randomUUID(),
                "Postgres", "Instructor", "instructor-" + UUID.randomUUID() + "@example.com", department.id(), now, now, 0));
        Course course = courses.create(new Course(UUID.randomUUID(), "PG" + UUID.randomUUID().toString().substring(0, 6),
                "Capacity", null, 3, 1, CourseStatus.OPEN, department.id(), instructor.id(), now, now, 0));
        Student first = student(department.id(), now);
        Student second = student(department.id(), now);

        var enrollment = enrollmentService.enroll(first.id(), course.id());
        assertThatThrownBy(() -> enrollmentService.enroll(second.id(), course.id())).isInstanceOf(ConflictException.class);
        assertThat(jdbc.queryForObject("select occupied_seats from courses where id = ?", Integer.class, course.id())).isEqualTo(1);
        assertThat(relationships.findCoursesByStudent(first.id(), new CursorRequest(20, null)).content())
                .extracting(Course::id).containsExactly(course.id());
        assertThat(enrollmentQueries.findByStudent(first.id(), null, null, new CursorRequest(20, null)).content())
                .extracting(com.abc.studentportal.enrollment.domain.Enrollment::id).containsExactly(enrollment.id());
        assertThat(courseQueries.findByDepartment(department.id(), new CursorRequest(20, null)).content())
                .extracting(Course::id).contains(course.id());

        enrollmentService.drop(enrollment.id(), enrollment.version());
        assertThat(jdbc.queryForObject("select occupied_seats from courses where id = ?", Integer.class, course.id())).isZero();
        assertThat(enrollmentService.enroll(second.id(), course.id())).isNotNull();
    }

    @Test
    void concurrentEnrollmentCannotOversubscribeLastSeat() throws Exception {
        Instant now = Instant.now();
        Department department = department("PG-RACE", now);
        Instructor instructor = instructors.create(new Instructor(UUID.randomUUID(), "EMP-" + UUID.randomUUID(),
                "Race", "Owner", "race-" + UUID.randomUUID() + "@example.com", department.id(), now, now, 0));
        Course course = courses.create(new Course(UUID.randomUUID(), "RC" + UUID.randomUUID().toString().substring(0, 6),
                "Last seat", null, 3, 1, CourseStatus.OPEN, department.id(), instructor.id(), now, now, 0));
        Student first = student(department.id(), now);
        Student second = student(department.id(), now);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> one = executor.submit(() -> enrollAfter(start, first.id(), course.id()));
            Future<Boolean> two = executor.submit(() -> enrollAfter(start, second.id(), course.id()));
            start.countDown();
            assertThat(java.util.List.of(one.get(), two.get())).containsExactlyInAnyOrder(true, false);
            assertThat(jdbc.queryForObject("select occupied_seats from courses where id = ?", Integer.class, course.id())).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void preservesSourceAuditStateAndRejectsStaleMutationVersions() {
        Instant created = Instant.parse("2024-01-02T03:04:05Z");
        Instant updated = Instant.parse("2025-02-03T04:05:06Z");
        UUID id = UUID.randomUUID();
        Department imported = departments.create(new Department(id, "IMP-" + id.toString().substring(0, 6),
                "Imported", null, created, updated, 3));
        assertThat(imported.createdAt()).isEqualTo(created);
        assertThat(imported.updatedAt()).isEqualTo(updated);
        assertThat(imported.version()).isEqualTo(3);

        Department changed = departments.update(new Department(id, imported.code(), "Changed", null,
                created, Instant.parse("2025-03-04T05:06:07Z"), imported.version()));
        assertThatThrownBy(() -> departments.delete(imported)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        departments.delete(changed);
    }

    private boolean enrollAfter(CountDownLatch start, UUID studentId, UUID courseId) throws InterruptedException {
        start.await();
        try {
            enrollmentService.enroll(studentId, courseId);
            return true;
        } catch (ConflictException exception) {
            return false;
        }
    }

    private Department department(String prefix, Instant now) {
        UUID id = UUID.randomUUID();
        return departments.create(new Department(id, prefix + "-" + id.toString().substring(0, 6), prefix, null, now, now, 0));
    }

    private Student student(UUID departmentId, Instant now) {
        UUID id = UUID.randomUUID();
        return students.create(new Student(id, "STU-" + id, "Test", "Student", "student-" + id + "@example.com",
                StudentStatus.ACTIVE, departmentId, now, now, 0));
    }
}
