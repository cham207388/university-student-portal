package com.abc.studentportal.common.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.application.PostgresCourseService;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.domain.Instructor;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

@Tag("localstack-rds")
@SpringBootTest
@ActiveProfiles("local-postgres")
class PostgresApplicationServiceIntegrationTest {

    @Autowired PostgresCourseService courses;
    @Autowired CourseRepository courseRepository;
    @Autowired DepartmentRepository departments;
    @Autowired InstructorRepository instructors;

    @Test
    void serviceUpdateRejectsStaleVersion() {
        Instant timestamp = Instant.now();
        UUID departmentId = UUID.randomUUID();
        Department department = departments.create(new Department(departmentId, "SVC-" + departmentId.toString().substring(0, 8),
                "Services", "Integration", timestamp, timestamp, 0));
        UUID instructorId = UUID.randomUUID();
        Instructor instructor = instructors.create(new Instructor(instructorId, "EMP-" + instructorId.toString().substring(0, 8),
                "Service", "Owner", "service-" + instructorId + "@example.com", departmentId,
                timestamp, timestamp, 0));

        Course created = courses.create(new PostgresCourseService.CreateCommand("SVC" + departmentId.toString().substring(0, 5).toUpperCase(),
                "Transactional services", "RDS", 3, 10, CourseStatus.OPEN, departmentId, instructorId));
        Course changed = courses.update(created.id(), new PostgresCourseService.UpdateCommand(created.courseCode(),
                "Updated title", created.description(), created.credits(), created.capacity(), departmentId,
                instructorId, created.version()));

        assertThat(changed.version()).isGreaterThan(created.version());
        assertThatThrownBy(() -> courses.update(created.id(), new PostgresCourseService.UpdateCommand(
                created.courseCode(), "Stale update", created.description(), created.credits(), created.capacity(),
                departmentId, instructorId, created.version())))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        courses.delete(changed.id(), changed.version());
        instructors.delete(instructor);
        departments.delete(department);
    }

    @Test
    void invalidRelationshipRollsBackBeforeCourseInsert() {
        UUID departmentId = UUID.randomUUID();
        UUID unrelatedDepartmentId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        Department department = departments.create(new Department(departmentId, "RB-" + departmentId.toString().substring(0, 8),
                "Rollback", "Integration", timestamp, timestamp, 0));
        Department unrelated = departments.create(new Department(unrelatedDepartmentId, "RU-" + unrelatedDepartmentId.toString().substring(0, 8),
                "Unrelated", "Integration", timestamp, timestamp, 0));
        UUID instructorId = UUID.randomUUID();
        Instructor instructor = instructors.create(new Instructor(instructorId, "EMP-" + instructorId.toString().substring(0, 8),
                "Wrong", "Department", "wrong-" + instructorId + "@example.com", unrelatedDepartmentId,
                timestamp, timestamp, 0));
        String courseCode = "RB" + departmentId.toString().substring(0, 6).toUpperCase();

        assertThatThrownBy(() -> courses.create(new PostgresCourseService.CreateCommand(courseCode, "Rejected", "RDS",
                3, 10, CourseStatus.OPEN, departmentId, instructorId)))
                .isInstanceOf(InvalidRequestException.class);
        assertThat(courseRepository.findByCourseCode(courseCode)).isEmpty();

        instructors.delete(instructor);
        departments.delete(unrelated);
        departments.delete(department);
    }
}
